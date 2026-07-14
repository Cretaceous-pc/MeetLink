<template>
  <div class="chat-container">
    <!--文件传输-->
    <file-transfer
      v-model:visible="fileInfo.fileVisible"
      :target-info="fileInfo.fileTargetInfo"
      :is-send="fileInfo.fileIsSend"
      :file="fileInfo.file"
    />
    <!--语音接听-->
    <video-chat
      v-model:visible="videoInfo.videoVisible"
      :target-info="videoInfo.videoTargetInfo"
      :is-send="videoInfo.videoIsSend"
      :is-only-audio="videoInfo.videoIsOnlyAudio"
    />
    <!--用户信息修改-->
    <modify-user-info v-model:is-open="modifyUserInfoIsOpen" />
    <!--通知-->
    <notify />
    <!--表情弹窗-->
    <linyu-popup v-model:visible="isEmojiVisible" :position="emojiPosition">
      <linyu-emoji-box @on-emoji="handlerOnEmoji" />
    </linyu-popup>
    <div class="chat-bg">
      <div class="chat-box">
        <!-- 左侧菜单 -->
        <div class="box-left" :class="{ 'show-left': showLeft }">
          <div class="chat-list-title">
            <div class="relative flex">
              消息列表

            </div>
            <div class="close-btn" @click="showLeft = false">×</div>
          </div>
          <!-- 群聊 -->
          <div
            v-if="groupChat"
            class="text-[rgba(var(--text-color),0.7)] font-[600] mb-[5px] mt-[6px]"
          >
            群聊
          </div>
          <div v-if="groupChat" class="chat-list-group">
            <div
              :class="['chat-list-item', targetId === roomId ? 'blue' : 'white']"
              @click="targetId = roomId; closeMask()"
            >
              <linyu-avatar :info="groupChat?.targetInfo || { name: '#' + roomId }" size="40px" class="mr-[10px]" />
              <div class="chat-item-content">
                <div class="flex items-center mb-[5px]">
                  <div class="chat-content-name">{{ groupChat?.targetInfo?.name || '#' + roomId }}</div>
                  <linyu-dot-hint
                    v-if="groupChat?.unreadCount > 0 && targetId !== roomId"
                    :text="groupChat.unreadCount"
                  />
                </div>
                <div class="chat-content-msg">
                  <linyu-chat-list-content :is-group="true" :msg="groupChat?.lastMessage" />
                </div>
              </div>
            </div>
          </div>
          <div
            v-if="privateChatList?.length > 0"
            class="text-[rgba(var(--text-color),0.7)] font-[600] mb-[5px]"
          >
            私聊
          </div>
          <div class="chat-list-content">
            <div
              v-for="item in privateChatList"
              :key="item.id"
              :class="['chat-list-item', targetId === item.targetId ? 'blue' : 'white']"
              @click="
                () => {
                  targetId = item.targetId
                  currentSelectTarget = item
                  closeMask()
                }
              "
            >
              <linyu-avatar :info="item.targetInfo" size="40px" class="mr-[10px]" />
              <div class="chat-item-content">
                <div class="flex items-center mb-[5px]">
                  <div class="chat-content-name">{{ item.targetInfo.name }}</div>
                  <linyu-dot-hint
                    v-if="item?.unreadCount > 0 && targetId !== item.targetId"
                    :text="item.unreadCount"
                  />
                </div>
                <div class="chat-content-msg">
                  <linyu-chat-list-content :is-group="false" :msg="item?.lastMessage" />
                </div>
              </div>
              <linyu-text-button
                v-if="targetId === item.targetId"
                text="移除"
                class="ml-[10px]"
                @click="() => onDeleteChatList(item.id)"
              />
            </div>
          </div>
          <div class="mb-[10px]">
            <linyu-card-carousel
              :play="true"
              :images="[]"
              @click="(card) => handlerCardClick(card)"
            />
          </div>
          <!-- 切换聊天室按钮 -->
          <div class="switch-room-btn" @click="openSwitchRoomDialog">
            <i class="iconfont icon-qiehuan mr-[6px] text-[14px]" />
            切换聊天室
          </div>
        </div>
        <!-- 遮罩层 -->
        <div class="mask" v-if="showLeft || showRight" @click="closeMask"></div>
        <!-- 中间部分 -->
        <div class="box-middle">
          <div class="middle-top">
            <div class="menu-btn" @click="handlerBackToRooms" title="返回房间列表">
              <i class="iconfont icon-fanhui text-[24px]" />
            </div>
            <template v-if="targetId === roomId">
              {{ groupChat?.targetInfo?.name || '#' + roomId }}（{{ msgStore.userListMap.size }}）
            </template>
            <template v-else>
              {{ currentSelectTarget?.targetInfo?.name }}
            </template>
            <div class="menu-btn" @click="showRight = true">
              <i class="iconfont icon-shezhi text-[24px]" />
            </div>
          </div>
          <div class="middle-content">
            <div class="chat-show-area" ref="chatShowAreaRef">
              <div
                v-for="item in msgRecord"
                class="msg-item"
                :key="item.id"
                :class="{ right: item.fromId === userInfoStore.userId }"
              >
                <linyu-msg :msg="item" :user="msgStore.userListMap.get(item.fromId)" />
              </div>
              <chat-skeleton v-if="isChatRecordLoading && msgRecord?.length <= 0 && !isComplete" />
              <div v-if="isSendLoading" class="flex w-full justify-center items-center">
                <linyu-loading label="发送中" />
              </div>
              <div v-if="currentNewMsgCount > 0" class="new-msg-count" @click="scrollToBottom">
                <img alt="" class="h-[18px] mr-[5px]" src="/down.png" />
                {{ currentNewMsgCount }} 条新消息
              </div>
            </div>
            <div class="chat-input-area">
              <div class="chat-input">
                <div v-if="msgStore.referenceMsg" class="reference-msg">
                  <div
                    class="reference-msg-content"
                    v-if="msgStore.referenceMsg.type === MessageType.Text"
                  >
                    {{ msgStore.referenceMsg.fromInfo.name }} :
                    <text-msg :msg="msgStore.referenceMsg" />
                  </div>
                  <div
                    v-if="msgStore.referenceMsg.type === MessageType.Emoji"
                    class="flex items-center"
                  >
                    <div>{{ msgStore.referenceMsg.fromInfo.name }} :</div>
                    <emoji-msg height="40px" width="40px" :src="msgStore.referenceMsg.message" />
                  </div>
                  <call-msg
                    v-if="msgStore.referenceMsg.type === MessageType.Call"
                    :msg="msgStore.referenceMsg"
                  />
                  <div class="ml-[10px]">
                    <linyu-icon-button
                      @click="msgStore.referenceMsg = null"
                      size="20px"
                      font-size="12px"
                      icon="icon-shanchu"
                    />
                  </div>
                </div>
                <div class="flex items-center">
                  <div class="emoji-button mr-[10px]" @click="handlerSetEmojiBoxPosition">
                    <div class="iconfont icon-biaoqing text-[28px]" />
                  </div>
                  <div class="chat-msg-input">
                    <linyu-msg-input
                      v-model:value="msgContent"
                      ref="msgInputRef"
                      @send="handlerSubmitMsg"
                      :user="userListAll"
                      :is-at-popup="targetId === roomId"
                    />
                  </div>
                  <div v-if="targetId !== roomId" class="flex gap-[2px]">
                    <div class="emoji-button" @click="fileInput.click()">
                      <input
                        type="file"
                        ref="fileInput"
                        @change="handlerSendFile"
                        style="display: none"
                      />
                      <div class="iconfont icon-wenjian text-[22px]" />
                    </div>
                    <div
                      class="emoji-button"
                      @click="() => handlerVideoCall(currentSelectTarget?.targetInfo, true, true)"
                    >
                      <div class="iconfont icon-yuyintonghua text-[22px]" />
                    </div>
                    <div
                      class="emoji-button"
                      @click="() => handlerVideoCall(currentSelectTarget?.targetInfo, true, false)"
                    >
                      <div class="iconfont icon-shipingtonghua text-[22px]" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="publish-button" @click="handlerSubmitMsg">
                <i class="iconfont icon-fasong2 text-[28px]" />
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧菜单 -->
        <div class="box-right" :class="{ 'show-right': showRight }">
          <div class="right-top">
            <div class="flex items-center">
              <linyu-avatar
                :info="{ name: userInfoStore.userName, avatar: userInfoStore.avatar }"
                size="40px"
                class="mr-[5px] cursor-pointer"
                @click="modifyUserInfoIsOpen = true"
              />
              <div class="user-name">{{ userInfoStore.userName }}</div>
            </div>
            <div class="flex">
              <linyu-icon-button
                v-if="themeStore.theme === 'light'"
                @click="(e) => toggleDark(e, 'dark')"
                icon="icon-taiyang"
              />
              <linyu-icon-button
                v-if="themeStore.theme === 'dark'"
                @click="(e) => toggleDark(e, 'light')"
                icon="icon-yueliang"
              />
              <linyu-icon-button @click="handlerLogout" icon="icon-tuichu" />
            </div>
          </div>
          <div class="right-content">
            <div class="flex justify-between items-center mb-[10px]">
              <div class="text-[rgb(var(--text-color))] text-[14px] font-[600]">
                在线人数（{{ onlineCount }}）
              </div>
              <linyu-input
                placeholder="搜索用户"
                height="30px"
                width="140px"
                radius="5px"
                font-size="14px"
                v-model:value="userSearchValue"
              />
            </div>
            <div class="online-list">
              <div
                v-for="(item, index) in userList"
                class="online-list-item"
                :key="item.id"
                :class="{ odd: index % 2 === 0 }"
              >
                <div class="online-item-content">
                  <div class="w-[40px] h-[40px] relative">
                    <linyu-avatar :info="item" size="40px" />
                    <div v-if="item.status?.length" class="online-status" />
                  </div>
                  <div class="online-username">{{ item.name }}</div>
                  <linyu-tooltip
                    v-if="item.status?.includes('web')"
                    content="浏览器在线"
                    class="ml-[2px]"
                  >
                    <img src="/badge/web-online.png" alt="" class="h-[18px]" draggable="false" />
                  </linyu-tooltip>
                  <linyu-tooltip
                    v-if="item.status?.includes('ssh')"
                    content="SSH在线"
                    class="ml-[2px]"
                  >
                    <img src="/badge/ssh-online.png" alt="" class="h-[18px]" />
                  </linyu-tooltip>
                </div>
                <div class="online-item-operation ml-[20px]">
                  <linyu-text-button
                    v-if="item.id !== userInfoStore.userId && item.type !== UserType.Bot"
                    text="私聊"
                    @click="
                      () => {
                        onCreatePrivateChat(item.id)
                        closeMask()
                      }
                    "
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 切换聊天室弹窗 -->
  <linyu-modal v-model:is-open="switchRoomVisible">
    <div class="modal-wrapper">
      <Transition
        @before-enter="onSwitchCardBeforeEnter"
        @enter="onSwitchCardEnter"
        @leave="onSwitchCardLeave"
        :css="false"
      >
        <div v-if="switchRoomVisible && switchRoomReady" class="switch-room-dialog">
          <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">切换聊天室</div>
          <div class="switch-room-list">
            <div
              v-for="room in switchRoomList"
              :key="room.roomId"
              :class="['switch-room-item', { active: room.roomId === roomId }]"
              @click="switchToRoom(room.roomId)"
            >
              <div class="flex-1">
                <div class="text-[15px] font-[600] text-[rgb(var(--text-color))]">
                  {{ room.name }}
                  <span class="text-[12px] font-mono text-[rgba(var(--text-color),0.4)] ml-[6px]">#{{ room.roomId }}</span>
                </div>
                <div class="text-[12px] text-[rgba(var(--text-color),0.5)] mt-[2px]" v-if="room.description">
                  {{ room.description }}
                </div>
                <div class="text-[12px] text-[rgba(var(--text-color),0.4)] mt-[2px]">
                  {{ room.onlineCount || 0 }} 在线 / {{ room.maxMembers }} 上限
                </div>
              </div>
              <linyu-text-button v-if="room.roomId !== roomId" text="进入" />
              <span v-else class="text-[12px] text-[rgb(var(--primary-color))] font-[600]">当前</span>
            </div>
            <div v-if="switchRoomList.length === 0" class="text-center py-[20px] text-[rgba(var(--text-color),0.4)]">
              暂无其他聊天室
            </div>
          </div>
          <div class="flex justify-end mt-[16px]">
            <linyu-button type="minor" style="width: 80px" @click="closeSwitchRoomDialog">关闭</linyu-button>
          </div>
        </div>
      </Transition>
    </div>
  </linyu-modal>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore.js'
import { toggleDark } from '@/utils/theme.js'
import LinyuInput from '@/components/LinyuInput.vue'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import LinyuAvatar from '@/components/LinyuAvatar.vue'
import ChatListApi from '@/api/chatList.js'
import LinyuDotHint from '@/components/LinyuDotHint.vue'
import MessageApi from '@/api/message.js'
import EventBus from '@/utils/eventBus.js'
import UserApi from '@/api/user.js'
import FileApi from '@/api/file.js'
import LinyuTooltip from '@/components/LinyuTooltip.vue'
import ws from '@/utils/ws.js'
import LinyuTextButton from '@/components/LinyuTextButton.vue'
import { useToast } from '@/components/ToastProvider.vue'
import LinyuIconButton from '@/components/LinyuIconButton.vue'
import LinyuMsg from '@/components/Msg/LinyuMsg.vue'
import { useChatMsgStore } from '@/stores/useChatMsgStore.js'
import LinyuPopup from '@/components/LinyuPopup.vue'
import LinyuEmojiBox from '@/components/LinyuEmojiBox.vue'
import EmojiMsg from '@/components/Msg/MsgContent/EmojiMsg.vue'
import LinyuChatListContent from '@/components/Msg/LinyuChatListContent.vue'
import { MessageType } from '@/constant/messageType.js'
import { MessageSource } from '@/constant/messageSource.js'
import LinyuMsgInput from '@/components/LinyuMsgInput.vue'
import { UserType } from '@/constant/userType.js'
import TextMsg from '@/components/Msg/MsgContent/TextMsg.vue'
import VideoChat from '@/components/VideoChat.vue'
import VideoApi from '@/api/video.js'
import CallMsg from '@/components/Msg/MsgContent/CallMsg.vue'
import FileTransfer from '@/components/FileTransfer.vue'
import LinyuLabel from '@/components/LinyuLabel.vue'
import ModifyUserInfo from '@/components/ModifyUserInfo.vue'
import { useUserInfoStore } from '@/stores/useUserInfoStore.js'
import LinyuLoading from '@/components/LinyuLoading.vue'
import ChatSkeleton from '@/components/ChatSkeleton.vue'
import LinyuCardCarousel from '@/components/LinyuCardCarousel.vue'
import Notify from '@/components/Notify.vue'
import LinyuModal from '@/components/LinyuModal.vue'
import LinyuButton from '@/components/LinyuButton.vue'
import RoomApi from '@/api/room'
import { gsap } from 'gsap'

const themeStore = useThemeStore()
const msgStore = useChatMsgStore()
const userInfoStore = useUserInfoStore()
const router = useRouter()
const route = useRoute()
const roomId = computed(() => route.params.roomId)
const showToast = useToast()
let cursorTime = null
let cursorId = null
const showLeft = ref(false)
const showRight = ref(false)
const groupChat = ref()
const msgRecord = ref([])
const targetId = ref()
const currentSelectTarget = ref()
const msgContent = ref('')
const chatShowAreaRef = ref()
const isLoading = ref(false)
const isComplete = ref(false)
const onlineCount = ref(0)
const privateChatList = ref([])
const userSearchValue = ref('')
const currentNewMsgCount = ref(0)
const isEmojiVisible = ref(false)
const emojiPosition = ref()
const msgInputRef = ref()
const videoInfo = reactive({
  videoVisible: false,
  videoTargetInfo: null,
  videoIsSend: false,
  videoIsOnlyAudio: false,
})
const fileInfo = reactive({
  fileVisible: false,
  fileTargetInfo: null,
  fileIsSend: false,
  file: null,
})
const fileInput = ref()
const modifyUserInfoIsOpen = ref(false)
const isSendLoading = ref(false)
let chatRecordLoadingTimer = null
const isChatRecordLoading = ref(false)

// --- 切换聊天室 ---
const switchRoomVisible = ref(false)
const switchRoomReady = ref(false)
const switchRoomList = ref([])

async function openSwitchRoomDialog() {
  switchRoomVisible.value = true
  await nextTick()
  switchRoomReady.value = true
  // 加载房间列表
  try {
    const res = await RoomApi.list()
    if (res.code === 0) switchRoomList.value = res.data
  } catch {}
}

function closeSwitchRoomDialog() {
  switchRoomReady.value = false
  setTimeout(() => { switchRoomVisible.value = false }, 200)
}

function switchToRoom(roomId) {
  closeSwitchRoomDialog()
  ws.disConnect()
  router.push('/chat/' + roomId)
}

// GSAP 动画钩子
function onSwitchCardBeforeEnter(el) { gsap.set(el, { scale: 0.92, autoAlpha: 0, y: 16 }) }
function onSwitchCardEnter(el, done) {
  gsap.to(el, { scale: 1, autoAlpha: 1, y: 0, duration: 0.35, ease: 'back.out(1.4)', onComplete: done })
}
function onSwitchCardLeave(el, done) {
  gsap.to(el, { scale: 0.95, autoAlpha: 0, y: -10, duration: 0.18, ease: 'power2.in', onComplete: done })
}

msgStore.$subscribe(() => {
  const { scrollTop, clientHeight, scrollHeight } = chatShowAreaRef.value
  const isBottom = scrollTop + clientHeight >= scrollHeight - 1
  if (isBottom) {
    scrollToBottom()
  }
})

const handleScroll = () => {
  if (chatShowAreaRef.value) {
    //最底部更新currentNewMsgCount为0
    const { scrollTop, clientHeight, scrollHeight } = chatShowAreaRef.value
    const isBottom = scrollTop + clientHeight >= scrollHeight - 1
    if (isBottom) currentNewMsgCount.value = 0
    if (chatShowAreaRef.value.scrollTop === 0 && !isLoading.value) {
      onGetMsgRecord()
    }
  }
}

const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024 // 5GB，P2P 直传不经过服务器

const handlerSendFile = (event) => {
  const files = event.target.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file.size > MAX_FILE_SIZE) {
      showToast('文件不能超过 5GB~', true)
      event.target.value = ''
      return
    }
    const targetInfo = currentSelectTarget.value?.targetInfo
    if (!targetInfo) {
      showToast('未选择发送目标~', true)
      event.target.value = ''
      return
    }
    fileInfo.fileVisible = true
    fileInfo.fileTargetInfo = targetInfo
    fileInfo.fileIsSend = true
    fileInfo.file = file
    FileApi.invite({
      userId: targetInfo.id,
      fileInfo: { name: file.name, size: file.size },
    }).catch(console.error)
    event.target.value = ''
  } else {
    showToast('文件不正确~', true)
  }
}

const handlerVideoCall = (info, isSend, isOnlyAudio) => {
  if (!info) return
  VideoApi.invite({ userId: info.id, isOnlyAudio: isOnlyAudio }).catch(console.error)
  videoInfo.videoVisible = true
  videoInfo.videoTargetInfo = info
  videoInfo.videoIsSend = isSend
  videoInfo.videoIsOnlyAudio = isOnlyAudio
}

const userList = computed(() => {
  const values = Array.from(msgStore.userListMap.values())
  if (userSearchValue.value) {
    return values.filter((item) => item.name.includes(userSearchValue.value))
  } else {
    return values
  }
})

const userListAll = computed(() => {
  const values = []
  for (const [, value] of msgStore.userListMap) {
    values.push({ name: value.name, id: value.id, type: value.type })
  }
  return values
})

const handlerSetEmojiBoxPosition = (e) => {
  const rect = e.target.getBoundingClientRect()
  emojiPosition.value = { top: rect.top, left: rect.left }
  isEmojiVisible.value = !isEmojiVisible.value
}

const handlerOnEmoji = (emoji, type) => {
  isEmojiVisible.value = false
  if (type === 'text') {
    nextTick(() => {
      msgInputRef.value.insertInputText(emoji)
    })
  } else {
    //图片链接
    let msg = {
      content: emoji,
      type: 'emoji',
    }
    onSendMsg(msg)
  }
}

//接收到消息
const handlerReceiveMsg = (data) => {
  handlerUpdateChatList(data)
  if (data.type === MessageType.Recall) {
    handlerReceiveRecallMsg(data)
    return
  }
  if (data.fromId === userInfoStore.userId && data.type !== MessageType.Call) return
  const { scrollTop, clientHeight, scrollHeight } = chatShowAreaRef.value
  const isBottom = scrollTop + clientHeight >= scrollHeight - 1
  if (
    (data.source === MessageSource.User && targetId.value === data.fromId) ||
    (data.source === MessageSource.User && targetId.value === data.toId) ||
    (data.source === MessageSource.Group && targetId.value === roomId.value)
  ) {
    msgRecord.value.push(data)
    //是否在最底部
    if (isBottom) {
      scrollToBottom()
    } else {
      currentNewMsgCount.value = currentNewMsgCount.value + 1
    }
  }
}

//更新聊天列表信息
const handlerUpdateChatList = (message) => {
  if (message.source === MessageSource.Group && message.toId === roomId.value) {
    groupChat.value.unreadCount = groupChat.value.unreadCount + 1
    groupChat.value.lastMessage = message
    return
  }
  let i = 0
  while (i < privateChatList.value.length) {
    let chat = privateChatList.value[i]
    if (message.fromId === chat.targetId || message.toId === chat.targetId) {
      chat.unreadCount = chat.unreadCount + 1
      chat.lastMessage = message
      break
    }
    i++
  }
  if (i >= privateChatList.value.length) {
    onGetPrivateChatList()
  }
}

//接收到通知
const handlerReceiveNotify = (data) => {
  let user = msgStore.userListMap.get(data.content.id)
  if (!user) {
    user = data.content
    msgStore.userListMap.set(user.id, user)
  }
  switch (data.type) {
    case 'web-online':
      if (!user.status) {
        user.status = ['web']
        handlerUserListSort()
      } else if (!user.status.includes('web')) {
        user.status = [...user.status, 'web']
        handlerUserListSort()
      }
      break
    case 'web-offline':
      if (user.status) {
        user.status = user.status.filter((status) => status !== 'web')
        handlerUserListSort()
      }
      break
  }
}

const handlerVideoMsg = async (msg) => {
  if (msg.fromId === userInfoStore.userId) return
  if (msg.type === 'invite') {
    const targetInfo = msgStore.userListMap.get(msg.fromId)
    videoInfo.videoVisible = true
    videoInfo.videoTargetInfo = targetInfo
    videoInfo.videoIsSend = false
    videoInfo.videoIsOnlyAudio = msg.isOnlyAudio
  }
}

const handlerFileMsg = async (msg) => {
  if (msg.fromId === userInfoStore.userId) return
  if (msg.type === 'invite') {
    const targetInfo = msgStore.userListMap.get(msg.fromId)
    fileInfo.fileVisible = true
    fileInfo.fileTargetInfo = targetInfo
    fileInfo.fileIsSend = false
    fileInfo.file = msg.fileInfo
  }
}

onMounted(async () => {
  EventBus.on('on-receive-msg', handlerReceiveMsg)
  EventBus.on('on-receive-video', handlerVideoMsg)
  EventBus.on('on-receive-file', handlerFileMsg)
  EventBus.on('on-room-disband', handlerRoomDisband)
  if (chatShowAreaRef.value) {
    chatShowAreaRef.value.addEventListener('scroll', handleScroll)
  }
  onGetGroupChat()
  onGetPrivateChatList()
  await onGetUserListMap()
  await onGetOnlineWeb()
  targetId.value = roomId.value
  // 连接 WebSocket 到当前房间
  let token = window.localStorage.getItem('x-token')
  if (token) ws.connect(token, roomId.value)
  EventBus.on('on-receive-notify', handlerReceiveNotify)
  handlerUserListSort()
})

onUnmounted(() => {
  EventBus.off('on-receive-msg', handlerReceiveMsg)
  EventBus.off('on-receive-notify', handlerReceiveNotify)
  EventBus.off('on-receive-file', handlerFileMsg)
  EventBus.off('on-receive-video', handlerVideoMsg)
  EventBus.off('on-room-disband', handlerRoomDisband)
})

const scrollToBottom = () => {
  if (chatShowAreaRef.value) {
    nextTick(() => (chatShowAreaRef.value.scrollTop = chatShowAreaRef.value.scrollHeight))
  }
}

//卡片点击（预留）
const handlerCardClick = () => {}

//获取聊天记录
const onGetMsgRecord = () => {
  if (isLoading.value || isComplete.value || !targetId.value) return
  isLoading.value = true

  const container = chatShowAreaRef.value
  const scrollTopBeforeLoad = container ? container.scrollTop : 0
  const scrollHeightBeforeLoad = container ? container.scrollHeight : 0

  MessageApi.record({ targetId: targetId.value, cursorTime, cursorId, num: 20 })
    .then((res) => {
      if (res.code === 0) {
        const newMessages = res.data
        if (newMessages.length > 0) {
          msgRecord.value = [...newMessages.reverse(), ...msgRecord.value]
          // 更新游标为本次返回中最旧的消息（server 返回 DESC，取最后一条）
          const oldestMsg = newMessages[newMessages.length - 1]
          cursorTime = oldestMsg.createTime
          cursorId = oldestMsg.id
          nextTick(() => {
            if (msgRecord.value.length === newMessages.length) {
              scrollToBottom()
            } else {
              requestAnimationFrame(() => {
                if (container) {
                  const scrollHeightAfterLoad = container.scrollHeight
                  container.scrollTop =
                    scrollTopBeforeLoad + (scrollHeightAfterLoad - scrollHeightBeforeLoad)
                }
              })
            }
          })
        } else {
          isComplete.value = true
        }
      }
    })
    .finally(() => {
      isLoading.value = false
      if (chatRecordLoadingTimer) {
        clearTimeout(chatRecordLoadingTimer)
        chatRecordLoadingTimer = null
      }
    })
}

//已读会话
const onReadChatList = (id) => {
  if (id === roomId.value && groupChat.value) {
    groupChat.value.unreadCount = 0
    return
  }
  for (let i = 0; i < privateChatList.value.length; i++) {
    let chat = privateChatList.value[i]
    if (id === chat.targetId) {
      chat.unreadCount = 0
      break
    }
  }
  ChatListApi.read({ targetId: id })
}

watch(
  targetId,
  (newValue, oldValue) => {
    msgContent.value = ''
    cursorTime = null
    cursorId = null
    msgRecord.value = []
    isComplete.value = false
    isLoading.value = false
    isChatRecordLoading.value = false
    chatRecordLoadingTimer = setTimeout(() => {
      isChatRecordLoading.value = true
    }, 100)
    onGetMsgRecord()
    onReadChatList(oldValue ?? newValue)
    // 切回群聊时也清零群聊未读
    if (newValue === roomId.value) onReadChatList(newValue)
  },
  { immediate: true },
)

watch(roomId, (newRoomId, oldRoomId) => {
  if (newRoomId === oldRoomId) return
  targetId.value = newRoomId
  onGetGroupChat()
  onGetUserListMap()
  onGetOnlineWeb()
  let token = window.localStorage.getItem('x-token')
  if (token) ws.connect(token, newRoomId)
})

const closeMask = () => {
  showLeft.value = false
  showRight.value = false
}

//退出登录
const handlerLogout = () => {
  localStorage.removeItem('x-token')
  userInfoStore.clearUserInfo()
  ws.disConnect()
  router.push('/login')
}

//返回房间列表
const handlerBackToRooms = () => {
  ws.disConnect()
  router.push('/rooms')
}

//房间被解散
const handlerRoomDisband = () => {
  showToast('聊天室已被管理员解散', 'warning')
  ws.disConnect()
  router.push('/rooms')
}

//获取群聊
const onGetGroupChat = async () => {
  try {
    const res = await ChatListApi.group(roomId.value)
    if (res.code === 0) {
      groupChat.value = res.data
    }
  } catch (e) {
    console.error('获取群聊信息失败', e)
  }
}

const handlerSubmitMsg = () => {
  if (!msgContent.value.toString().trim()) return
  let msg = {
    content: JSON.stringify(msgInputRef.value.getNodeList()),
    type: MessageType.Text,
  }
  onSendMsg(msg)
}

//发送消息
const onSendMsg = (msg) => {
  const time = setTimeout(() => {
    isSendLoading.value = true
    scrollToBottom()
  }, 300)
  MessageApi.send({
    targetId: targetId.value,
      source: targetId.value === roomId.value ? MessageSource.Group : MessageSource.User,
    msgContent: msg.content,
    referenceMsgId: msgStore.referenceMsg?.id,
    type: msg.type,
  })
    .then((res) => {
      if (res.code === 0) {
        msgRecord.value.push(res.data)
        scrollToBottom()
      }
    })
    .finally(() => {
      isSendLoading.value = false
      clearTimeout(time)
    })
  msgContent.value = ''
  msgStore.referenceMsg = null
}

//删除私聊列表内容
const onDeleteChatList = (id) => {
  ChatListApi.delete({ chatListId: id }).then((res) => {
    if (res.code === 0) {
      onGetPrivateChatList()
    }
  })
}

//获取用户列表
const onGetUserListMap = async () => {
  await UserApi.listMap().then((res) => {
    if (res.code === 0) {
      msgStore.setUserListMap(new Map(Object.entries(res.data)))
    }
  })
}

//获取web在线人员
const onGetOnlineWeb = async () => {
  await UserApi.onlineWeb().then((res) => {
    if (res.code === 0) {
      const onlineWeb = res.data
      for (let i = 0; i < onlineWeb?.length; i++) {
        const user = msgStore.userListMap.get(onlineWeb[i])
        user.status = Array.isArray(user.status) ? [...user.status, 'web'] : ['web']
      }
    }
  })
}

//排序
const handlerUserListSort = () => {
  const sortedEntries = [...msgStore.userListMap.entries()].sort(([, a], [, b]) => {
    if (a?.type === UserType.Bot && b?.type !== UserType.Bot) return -1
    if (b?.type === UserType.Bot && a?.type !== UserType.Bot) return 1
    const aStatus = a?.status || []
    const bStatus = b?.status || []
    const aStatusEmpty = aStatus.length === 0
    const bStatusEmpty = bStatus.length === 0
    if (aStatusEmpty && !bStatusEmpty) return 1
    if (!aStatusEmpty && bStatusEmpty) return -1
    return 0
  })
  msgStore.userListMap = new Map(sortedEntries)
  let onlineNum = 0
  for (let [, value] of msgStore.userListMap) {
    if (value.type === UserType.Bot) {
      continue
    }
    if (value?.status?.length > 0) {
      onlineNum++
    } else {
      break
    }
  }
  onlineCount.value = onlineNum
}

//接受到已撤回的消息
const handlerReceiveRecallMsg = (msg) => {
  for (let i = msgRecord.value.length - 1; i >= 0; i--) {
    if (msgRecord.value[i].id === msg.id) {
      msgRecord.value[i].type = msg.type
      msgRecord.value[i].message = msg.message
      break
    }
  }
}

//获取私聊列表
const onGetPrivateChatList = async () => {
  await ChatListApi.privateList().then((res) => {
    if (res.code === 0) {
      privateChatList.value = res.data
    }
  })
}

//创建私聊
const onCreatePrivateChat = (id) => {
  ChatListApi.create({ targetId: id }).then(async (res) => {
    if (res.code === 0) {
      currentSelectTarget.value = res.data
      await onGetPrivateChatList()
      targetId.value = id
    }
  })
}
</script>

<style lang="less" scoped>
.chat-container {
  width: 100%;
  height: 100%;
  position: absolute;
  background: var(--screen-bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .chat-bg {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background-image: var(--scrren-grid-bg-color);
    background-size: 50px 50px;
  }

  .mask {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.1);
    z-index: 10;
  }

  .chat-box {
    width: 80%;
    height: 90%;
    display: flex;
    position: relative;
    min-width: 0;

    @media screen and (max-width: 900px) {
      width: 95%;
      height: 95%;
    }

    .box-left {
      width: 280px;
      min-width: 280px;
      background-color: rgba(var(--background-color), 0.5);
      backdrop-filter: blur(8px);
      margin-right: 5px;
      border-radius: 5px;
      border: rgba(var(--background-color), 0.5) 3px solid;
      display: flex;
      flex-direction: column;
      padding: 0 10px;

      @media screen and (max-width: 900px) {
        position: fixed;
        left: -280px;
        top: 0;
        bottom: 0;
        margin: 0;
        z-index: 11;
        transition: all 0.3s;
        background-color: rgba(var(--background-color), 0.9);

        &.show-left {
          left: 0;
        }
      }

      .vcard {
        cursor: pointer;
        height: 60px;
        border-radius: 5px;
        background-size: cover;
        background-repeat: no-repeat;
        background-position: center;
      }

      .chat-list-title {
        color: rgb(var(--text-color));
        height: 60px;
        min-height: 60px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        line-height: 18px;
        font-weight: 600;
        user-select: none;
        @media screen and (max-width: 900px) {
          justify-content: space-between;
        }
      }

      .chat-list-item {
        height: 60px;
        margin-bottom: 5px;
        border-radius: 5px;
        display: flex;
        align-items: center;
        padding: 10px;
        cursor: pointer;
        user-select: none;

        .chat-item-content {
          display: flex;
          flex-direction: column;
          justify-content: space-between;
          flex: 1;
          overflow: hidden;

          .chat-content-name {
            font-size: 14px;
            color: white;
          }

          .chat-content-msg {
            width: 100%;
            font-size: 12px;
            color: rgba(255, 255, 255, 0.8);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }

        &.blue {
          background-color: rgb(var(--primary-color));

          &:hover {
            background-color: rgba(var(--primary-color), 0.7);
          }
        }

        &.black {
          background-image: url('/group-bg.png');
          background-size: cover;
          background-repeat: no-repeat;
          background-position: center;
        }

        &.white {
          background-color: rgba(var(--background-color), 0.5);

          .chat-item-content {
            .chat-content-name {
              color: rgb(var(--text-color));
            }

            .chat-content-msg {
              color: rgba(var(--text-color), 0.6);
            }
          }

          &:hover {
            background-color: rgba(var(--background-color), 0.8);
          }
        }
      }
    }

    .chat-list-group {
      margin-bottom: 6px;
    }

    .chat-list-content {
      flex: 1;
      overflow-y: scroll;
      padding-right: 5px;
      margin-right: -10px;
      margin-bottom: 20px;
    }

    .box-middle {
      min-width: 300px;
      flex: 1;
      margin-right: 5px;
      border-radius: 5px;
      display: flex;
      flex-direction: column;

      @media screen and (max-width: 900px) {
        margin: 0;
      }

      .middle-top {
        height: 60px;
        min-height: 60px;
        background-color: rgba(var(--background-color), 0.5);
        backdrop-filter: blur(8px);
        margin-bottom: 5px;
        border-radius: 5px;
        border: rgba(var(--background-color), 0.5) 3px solid;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 15px;
        font-size: 18px;
        font-weight: 600;
        user-select: none;
        color: rgb(var(--text-color));

        .menu-btn {
          cursor: pointer;
          font-size: 24px;
        }

        @media screen and (max-width: 900px) {
          justify-content: space-between;
        }

        @media screen and (min-width: 900px) {
          .menu-btn {
            display: none;
          }
        }
      }

      .middle-content {
        flex: 1;
        min-height: 300px;
        border-radius: 5px;
        background-image: linear-gradient(
          130deg,
          rgba(var(--background-color), 0.3),
          rgba(var(--background-color), 0.5)
        );
        backdrop-filter: blur(10px);
        border: rgba(var(--background-color), 0.5) 3px solid;
        display: flex;
        flex-direction: column;

        .chat-show-area {
          flex: 1;
          padding: 10px;
          display: flex;
          flex-direction: column;
          overflow-y: scroll;

          .msg-item {
            display: flex;
          }

          .new-msg-count {
            position: fixed;
            right: 15px;
            bottom: 80px;
            padding: 4px 15px;
            border-radius: 20px;
            background-color: rgba(var(--background-color), 0.8);
            backdrop-filter: blur(10px);
            color: rgba(var(--primary-color));
            font-size: 14px;
            user-select: none;
            border: rgba(var(--background-color)) 2px solid;
            font-weight: 600;
            display: flex;
            justify-content: center;
            align-items: center;
            cursor: pointer;
          }
        }

        .chat-input-area {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 0 10px;
          margin: 15px 0;

          .chat-input {
            width: 80%;
            background-color: rgba(var(--background-color));
            border-radius: 10px;
            overflow: hidden;
            padding: 10px;
            display: flex;
            flex-direction: column;

            .chat-msg-input {
              flex: 1;
              width: 0;
            }

            .emoji-button {
              width: 28px;
              height: 28px;
              user-select: none;
              cursor: pointer;
              display: flex;
              justify-content: center;
              align-items: center;
              color: rgba(var(--text-color), 0.5);
              flex-shrink: 1;
            }

            .reference-msg {
              font-size: 14px;
              display: flex;
              align-items: center;
              justify-content: space-between;
              background-color: rgba(var(--text-color), 0.1);
              color: rgba(var(--text-color), 0.9);
              border-radius: 5px;
              padding: 5px 10px;
              margin-bottom: 5px;
              user-select: none;

              .reference-msg-content {
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
                word-break: break-word;
              }
            }
          }

          .publish-button {
            height: 55px;
            width: 55px;
            border-radius: 50px;
            background-color: rgb(var(--primary-color));
            border: rgba(var(--background-color), 0.5) 2px solid;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #ffffff;
            cursor: pointer;
            margin-left: 10px;
          }
        }
      }
    }

    .box-right {
      width: 280px;
      min-width: 280px;
      border-radius: 5px;
      display: flex;
      flex-direction: column;

      @media screen and (max-width: 900px) {
        position: fixed;
        right: -280px;
        top: 0;
        bottom: 0;
        margin: 0;
        z-index: 11;
        transition: all 0.3s;

        &.show-right {
          right: 0;
        }

        .right-top {
          background-color: rgba(var(--background-color), 0.9) !important;
        }

        .right-content {
          background-color: rgba(var(--background-color), 0.9) !important;
        }
      }

      .right-top {
        height: 60px;
        min-height: 60px;
        background-color: rgba(var(--background-color), 0.5);
        backdrop-filter: blur(8px);
        margin-bottom: 5px;
        border-radius: 5px;
        border: rgba(var(--background-color), 0.5) 3px solid;
        display: flex;
        align-items: center;
        padding: 5px;
        user-select: none;
        justify-content: space-between;

        .user-name {
          font-size: 16px;
          font-weight: 600;
          color: rgb(var(--text-color));
        }

        .user-operate {
          font-size: 14px;
          font-weight: 600;
          color: rgb(var(--primary-color));
          cursor: pointer;
        }
      }

      .right-content {
        flex: 1;
        min-height: 300px;
        background-color: rgba(var(--background-color), 0.5);
        backdrop-filter: blur(8px);
        border-radius: 5px;
        border: rgba(var(--background-color), 0.5) 3px solid;
        padding: 5px;
        display: flex;
        flex-direction: column;

        .online-list {
          overflow-y: scroll;
          padding-right: 5px;
          margin-right: -5px;
          flex: 1;

          .online-list-item {
            height: 50px;
            border-radius: 5px;
            background-image: linear-gradient(
              to right,
              rgba(var(--minor-color), 0.2),
              rgba(var(--minor-color), 0)
            );
            margin-bottom: 5px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 10px;

            .online-item-content {
              display: flex;
              align-items: center;

              .online-username {
                max-width: 100px;
                margin-left: 10px;
                font-weight: 600;
                color: rgb(var(--text-color));
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
              }

              .online-status {
                position: absolute;
                width: 12px;
                height: 12px;
                border-radius: 10px;
                right: 0;
                bottom: 0;
                background-color: rgb(var(--primary-color));
                border: #ffffff 2px solid;
              }
            }

            .online-item-operation {
              align-items: center;
              opacity: 0;
              transition: opacity 0.5s ease;
              pointer-events: none;
            }

            &.odd {
              background-image: linear-gradient(
                to left,
                rgba(var(--minor-color), 0.2),
                rgba(var(--minor-color), 0)
              );
            }

            &:hover {
              .online-item-operation {
                opacity: 1;
                pointer-events: auto;
              }
            }

            @media screen and (max-width: 900px) {
              .online-item-operation {
                opacity: 1;
                pointer-events: auto;
              }
            }
          }
        }
      }
    }
  }

  .close-btn {
    cursor: pointer;
    font-size: 24px;
    padding: 0 10px;

    @media screen and (min-width: 900px) {
      display: none;
    }
  }
}

// --- 切换聊天室 ---
.switch-room-btn {
  margin-top: auto;
  margin-bottom: 10px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  cursor: pointer;
  user-select: none;
  font-size: 14px;
  font-weight: 600;
  color: rgb(var(--primary-color));
  border: 1px dashed rgba(var(--primary-color), 0.4);
  transition: all 0.2s;
  &:hover {
    background: rgba(var(--primary-color), 0.1);
    border-color: rgb(var(--primary-color));
  }
}

.modal-wrapper {
  width: 100vw; height: 100vh;
  display: flex; justify-content: center; align-items: center;
}

.switch-room-dialog {
  width: 480px;
  max-height: 70vh;
  overflow-y: auto;
  background-image: linear-gradient(130deg, rgba(var(--background-color), 0.6), rgba(var(--background-color), 0.85));
  backdrop-filter: blur(16px);
  border: rgba(var(--background-color), 0.6) 2px solid;
  border-radius: 10px;
  padding: 30px 35px;
  will-change: transform, opacity;
  @media screen and (max-width: 500px) { width: 90%; padding: 20px 18px; }
}

.switch-room-list { max-height: 400px; overflow-y: auto; }
.switch-room-item {
  display: flex; align-items: center;
  padding: 10px 12px; border-radius: 8px; margin-bottom: 4px;
  cursor: pointer; transition: background 0.15s;
  &:hover { background: rgba(var(--primary-color), 0.06); }
  &.active { background: rgba(var(--primary-color), 0.1); }
}
</style>
