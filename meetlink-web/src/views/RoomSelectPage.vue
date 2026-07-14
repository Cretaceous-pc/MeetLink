<template>
  <div class="room-select-container">
    <!-- 主题切换 -->
    <div class="operations">
      <icon-button
        v-if="themeStore.theme === 'light'"
        @click="(e) => toggleDark(e, 'dark')"
        icon="icon-taiyang"
      />
      <icon-button
        v-if="themeStore.theme === 'dark'"
        @click="(e) => toggleDark(e, 'light')"
        icon="icon-yueliang"
      />
    </div>

    <!-- 主体 -->
    <div class="room-bg">
      <img class="logo" alt="" :src="`/title-${themeStore.theme}.png`" draggable="false" />
      <div class="room-content">
        <div class="room-box">
          <!-- 头部 -->
          <div class="flex justify-between items-center mb-[20px]">
            <div>
              <div class="text-[28px] text-[rgb(var(--text-color))] font-[600] leading-[30px]">
                聊天室
              </div>
              <div class="text-[14px] text-[rgba(var(--text-color),0.6)] mt-[4px]">
                {{ userInfoStore.userName }} · 选择或创建聊天室
              </div>
            </div>
            <linyu-text-button text="退出登录" style="color: rgba(var(--text-color),0.5)" @click="handleLogout" />
          </div>

          <!-- 房间列表 -->
          <div class="room-list-area">
            <div v-if="rooms.length === 0" class="text-center py-[30px] text-[rgba(var(--text-color),0.4)] text-[14px]">
              暂无聊天室，点击下方按钮加入或创建
            </div>
            <div
              v-for="(room, i) in rooms"
              :key="room.roomId"
              :ref="el => roomRefs[i] = el"
              class="room-item"
              @click="enterRoom(room.roomId)"
            >
              <linyu-avatar :info="{ name: room.name }" size="42px" :color="2" />
              <div class="flex-1 ml-[12px]">
                <div class="text-[15px] font-[600] text-[rgb(var(--text-color))]">
                  {{ room.name }}
                </div>
                <div class="text-[12px] text-[rgba(var(--text-color),0.5)] mt-[2px]">
                  {{ room.onlineCount || 0 }} 在线
                  <template v-if="room.maxMembers"> / {{ room.maxMembers }} 上限</template>
                  <template v-if="room.description"> · {{ room.description }}</template>
                </div>
              </div>
              <linyu-text-button text="进入" @click.stop="enterRoom(room.roomId)" />
            </div>
          </div>

          <!-- 按钮区 -->
          <div class="flex gap-[10px] mt-[20px]">
            <div class="flex-1 room-action-btn" @click="openJoinDialog">添加聊天室</div>
            <div class="flex-1 room-action-btn create" @click="openCreateDialog">新建聊天室</div>
            <div
              v-if="userInfoStore.role === 'admin'"
              class="flex-1 room-action-btn admin"
              @click="openAdminPanel"
            >
              管理
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== 加入聊天室弹窗 ====== -->
    <linyu-modal v-model:is-open="showJoinDialog">
      <div class="modal-wrapper">
        <Transition
          @before-enter="onCardBeforeEnter"
          @enter="onCardEnter"
          @leave="onCardLeave"
          :css="false"
        >
          <div v-if="showJoinDialog && joinDialogReady" class="dialog-card" ref="joinCardRef">
            <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">添加聊天室</div>
            <linyu-input
              v-model:value="joinForm.roomId"
              placeholder="聊天室编号"
              class="mb-[10px]"
            />
            <linyu-input
              v-model:value="joinForm.password"
              type="password"
              placeholder="聊天室密码"
              class="mb-[10px]"
            />
            <div v-if="joinError" class="text-[#ff4c4c] text-[13px] mb-[10px]">{{ joinError }}</div>
            <div class="flex justify-end gap-[10px]">
              <linyu-button type="minor" style="width: 80px" @click="closeJoinDialog">取消</linyu-button>
              <linyu-button style="width: 80px" @click="handleJoin" :disabled="joinLoading">
                {{ joinLoading ? '...' : '加入' }}
              </linyu-button>
            </div>
          </div>
        </Transition>
      </div>
    </linyu-modal>

    <!-- ====== 新建聊天室弹窗 ====== -->
    <linyu-modal v-model:is-open="showCreateDialog">
      <div class="modal-wrapper">
        <Transition
          @before-enter="onCardBeforeEnter"
          @enter="onCardEnter"
          @leave="onCardLeave"
          :css="false"
        >
          <div v-if="showCreateDialog && createDialogReady" class="dialog-card" ref="createCardRef">
            <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">新建聊天室</div>
            <linyu-input v-model:value="createForm.roomId" placeholder="聊天室编号（如 tech-2024）" class="mb-[10px]" />
            <linyu-input v-model:value="createForm.name" placeholder="聊天室名称" class="mb-[10px]" />
            <linyu-input v-model:value="createForm.password" type="password" placeholder="设置聊天室密码" class="mb-[10px]" />
            <linyu-input v-model:value="createForm.description" placeholder="描述（可选）" class="mb-[10px]" />
            <linyu-input v-model:value="createForm.inviteCode" placeholder="邀请码（管理员发放）" class="mb-[10px]" />
            <div v-if="createError" class="text-[#ff4c4c] text-[13px] mb-[10px]">{{ createError }}</div>
            <div class="flex justify-end gap-[10px]">
              <linyu-button type="minor" style="width: 80px" @click="closeCreateDialog">取消</linyu-button>
              <linyu-button style="width: 80px" @click="handleCreate" :disabled="createLoading">
                {{ createLoading ? '...' : '创建' }}
              </linyu-button>
            </div>
          </div>
        </Transition>
      </div>
    </linyu-modal>

    <!-- ====== 管理面板弹窗 ====== -->
    <linyu-modal v-model:is-open="showAdminPanel">
      <div class="modal-wrapper">
        <Transition
          @before-enter="onCardBeforeEnter"
          @enter="onCardEnter"
          @leave="onCardLeave"
          :css="false"
        >
          <div v-if="showAdminPanel && adminDialogReady" class="dialog-card dialog-wide" ref="adminCardRef">
            <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">管理面板</div>

            <!-- 生成邀请码 -->
            <div class="admin-section">
              <div class="text-[14px] font-[600] text-[rgb(var(--text-color))] mb-[8px]">生成邀请码</div>
              <div class="flex items-center gap-[10px]">
                <select v-model="inviteMaxMembers" class="input-select">
                  <option :value="50">50 人</option>
                  <option :value="100">100 人</option>
                  <option :value="200">200 人</option>
                  <option :value="500">500 人</option>
                </select>
                <linyu-button style="width: 80px" @click="handleGenInviteCode">生成</linyu-button>
              </div>
            </div>

            <!-- 邀请码列表 -->
            <div class="admin-section">
              <div class="text-[14px] font-[600] text-[rgb(var(--text-color))] mb-[8px]">邀请码列表</div>
              <div class="max-h-[120px] overflow-y-auto">
                <div v-for="code in inviteCodes" :key="code.id" class="flex items-center gap-[10px] py-[4px] border-b border-[rgba(var(--text-color),0.08)] text-[13px]">
                  <span class="font-mono font-[600]" :class="{ 'line-through opacity-40': code.isUsed }">{{ code.code }}</span>
                  <span class="text-[rgba(var(--text-color),0.4)]">{{ code.maxMembers }}人</span>
                  <span v-if="code.isUsed" class="text-[#10b981]">已用({{ code.usedForGroup }})</span>
                  <span v-else class="text-[#3b82f6]">可用</span>
                  <linyu-text-button
                    v-if="!code.isUsed"
                    text="废弃"
                    style="color: #ff4c4c; font-size: 12px"
                    @click="handleInvalidate(code.id)"
                  />
                </div>
                <div v-if="inviteCodes.length === 0" class="text-[rgba(var(--text-color),0.3)] text-[13px] py-[10px]">暂无邀请码</div>
              </div>
            </div>

            <!-- 所有聊天室 -->
            <div class="admin-section">
              <div class="text-[14px] font-[600] text-[rgb(var(--text-color))] mb-[8px]">所有聊天室</div>
              <div class="max-h-[180px] overflow-y-auto">
                <div
                  v-for="room in allRooms"
                  :key="room.roomId"
                  class="flex items-center justify-between py-[6px] border-b border-[rgba(var(--text-color),0.08)] text-[13px]"
                >
                  <div class="flex items-center gap-[8px]">
                    <span class="font-mono font-[600] text-[rgb(var(--text-color))]">{{ room.roomId }}</span>
                    <span class="text-[rgb(var(--text-color))]">{{ room.name }}</span>
                    <span class="text-[rgba(var(--text-color),0.4)]">{{ room.memberCount }}成员 / {{ room.onlineCount }}在线</span>
                  </div>
                  <div class="flex items-center gap-[6px]">
                    <linyu-text-button text="修改" style="font-size: 12px" @click="openEditRoomDialog(room)" />
                    <linyu-text-button text="解散" style="color: #ff4c4c; font-size: 12px" @click="openDisbandConfirm(room.roomId)" />
                  </div>
                </div>
              </div>
            </div>

            <div class="flex justify-end mt-[16px]">
              <linyu-button type="minor" style="width: 80px" @click="closeAdminPanel">关闭</linyu-button>
            </div>
          </div>
        </Transition>
      </div>
    </linyu-modal>

    <!-- ====== 修改聊天室弹窗 ====== -->
    <linyu-modal v-model:is-open="showEditRoomDialog">
      <div class="modal-wrapper">
        <Transition
          @before-enter="onCardBeforeEnter"
          @enter="onCardEnter"
          @leave="onCardLeave"
          :css="false"
        >
          <div v-if="showEditRoomDialog && editRoomReady" class="dialog-card">
            <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">
              修改聊天室 #{{ editRoomTarget?.roomId }}
            </div>
            <linyu-input v-model:value="editRoomForm.name" placeholder="聊天室名称" class="mb-[10px]" />
            <linyu-input v-model:value="editRoomForm.password" type="password" placeholder="新密码（留空不修改）" class="mb-[10px]" />
            <linyu-input v-model:value="editRoomForm.description" placeholder="描述" class="mb-[10px]" />
            <linyu-input v-model:value="editRoomForm.maxMembers" placeholder="人数上限" class="mb-[10px]" />
            <div v-if="editRoomError" class="text-[#ff4c4c] text-[13px] mb-[10px]">{{ editRoomError }}</div>
            <div class="flex justify-end gap-[10px]">
              <linyu-button type="minor" style="width: 80px" @click="closeEditRoomDialog">取消</linyu-button>
              <linyu-button style="width: 80px" @click="submitEditRoom" :disabled="editRoomLoading">
                {{ editRoomLoading ? '...' : '保存' }}
              </linyu-button>
            </div>
          </div>
        </Transition>
      </div>
    </linyu-modal>

    <!-- ====== 确认解散弹窗 ====== -->
    <linyu-modal v-model:is-open="showDisbandConfirm">
      <div class="modal-wrapper">
        <Transition
          @before-enter="onCardBeforeEnter"
          @enter="onCardEnter"
          @leave="onCardLeave"
          :css="false"
        >
          <div v-if="showDisbandConfirm && disbandReady" class="dialog-card" style="width:380px">
            <div class="text-[20px] font-[600] text-[rgb(var(--text-color))] mb-[16px]">确认解散</div>
            <div class="text-[15px] text-[rgba(var(--text-color),0.7)] mb-[20px]">
              确定要解散聊天室 <span class="font-[600] text-[rgb(var(--text-color))]">#{{ disbandTarget }}</span> 吗？<br/>
              <span class="text-[#ff4c4c] text-[13px]">此操作不可撤销，所有成员将被移除。</span>
            </div>
            <div class="flex justify-end gap-[10px]">
              <linyu-button type="minor" style="width: 80px" @click="closeDisbandConfirm">取消</linyu-button>
              <linyu-button style="width: 80px; background-color: #ff4c4c" @click="submitDisband" :disabled="disbandLoading">
                {{ disbandLoading ? '...' : '解散' }}
              </linyu-button>
            </div>
          </div>
        </Transition>
      </div>
    </linyu-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserInfoStore } from '@/stores/useUserInfoStore'
import { useThemeStore } from '@/stores/useThemeStore'
import { toggleDark } from '@/utils/theme'
import IconButton from '@/components/LinyuIconButton.vue'
import LinyuInput from '@/components/LinyuInput.vue'
import LinyuButton from '@/components/LinyuButton.vue'
import LinyuTextButton from '@/components/LinyuTextButton.vue'
import LinyuAvatar from '@/components/LinyuAvatar.vue'
import LinyuModal from '@/components/LinyuModal.vue'
import RoomApi from '@/api/room'
import { gsap } from 'gsap'

const router = useRouter()
const userInfoStore = useUserInfoStore()
const themeStore = useThemeStore()

const rooms = ref([])
const roomRefs = ref([])

// --- Dialog state ---
const showJoinDialog = ref(false)
const joinDialogReady = ref(false)
const joinCardRef = ref(null)

const showCreateDialog = ref(false)
const createDialogReady = ref(false)
const createCardRef = ref(null)

const showAdminPanel = ref(false)
const adminDialogReady = ref(false)
const adminCardRef = ref(null)

const joinForm = reactive({ roomId: '', password: '' })
const joinError = ref('')
const joinLoading = ref(false)

const createForm = reactive({ roomId: '', name: '', password: '', description: '', inviteCode: '' })
const createError = ref('')
const createLoading = ref(false)

const allRooms = ref([])
const inviteCodes = ref([])
const inviteMaxMembers = ref(100)

// --- GSAP card entrance animation ---
const ANIM_DURATION = 0.35

function onCardBeforeEnter(el) {
  gsap.set(el, { scale: 0.92, autoAlpha: 0, y: 16 })
}

function onCardEnter(el, done) {
  gsap.to(el, {
    scale: 1,
    autoAlpha: 1,
    y: 0,
    duration: ANIM_DURATION,
    ease: 'back.out(1.4)',
    onComplete: done,
  })
}

function onCardLeave(el, done) {
  gsap.to(el, {
    scale: 0.95,
    autoAlpha: 0,
    y: -10,
    duration: 0.18,
    ease: 'power2.in',
    onComplete: done,
  })
}

// --- Open / Close dialogs with animation support ---
async function openJoinDialog() {
  showJoinDialog.value = true
  await nextTick()
  joinDialogReady.value = true
  joinError.value = ''
}

function closeJoinDialog() {
  joinDialogReady.value = false
  setTimeout(() => {
    showJoinDialog.value = false
    joinForm.roomId = ''
    joinForm.password = ''
  }, 200)
}

async function openCreateDialog() {
  showCreateDialog.value = true
  await nextTick()
  createDialogReady.value = true
  createError.value = ''
}

function closeCreateDialog() {
  createDialogReady.value = false
  setTimeout(() => {
    showCreateDialog.value = false
    Object.assign(createForm, { roomId: '', name: '', password: '', description: '', inviteCode: '' })
  }, 200)
}

async function openAdminPanel() {
  showAdminPanel.value = true
  await nextTick()
  adminDialogReady.value = true
  await loadAdminData()
}

function closeAdminPanel() {
  adminDialogReady.value = false
  setTimeout(() => { showAdminPanel.value = false }, 200)
}

// --- Lifecycle ---
onMounted(async () => {
  await loadRooms()
  // 房间列表交错入场
  await nextTick()
  const items = roomRefs.value.filter(Boolean)
  if (items.length) {
    gsap.from(items, {
      autoAlpha: 0,
      x: -20,
      stagger: 0.06,
      duration: 0.3,
      ease: 'power2.out',
    })
  }
})

async function loadRooms() {
  try {
    const res = await RoomApi.list()
    if (res.code === 0) rooms.value = res.data
  } catch {}
}

function enterRoom(roomId) {
  router.push('/chat/' + roomId)
}

async function handleJoin() {
  joinError.value = ''
  if (!joinForm.roomId || !joinForm.password) { joinError.value = '请填写完整信息'; return }
  joinLoading.value = true
  try {
    const res = await RoomApi.join(joinForm)
    if (res.code === 0) {
      closeJoinDialog()
      await loadRooms()
      await nextTick()
      gsap.from(roomRefs.value.filter(Boolean), {
        autoAlpha: 0, x: -20, stagger: 0.06, duration: 0.3, ease: 'power2.out',
      })
    } else {
      joinError.value = res.msg || '加入失败'
    }
  } catch (e) {
    joinError.value = e?.response?.data?.msg || '加入失败'
  } finally {
    joinLoading.value = false
  }
}

async function handleCreate() {
  createError.value = ''
  if (!createForm.roomId || !createForm.name || !createForm.password || !createForm.inviteCode) {
    createError.value = '请填写完整信息'; return
  }
  createLoading.value = true
  try {
    const res = await RoomApi.create(createForm)
    if (res.code === 0) {
      closeCreateDialog()
      await loadRooms()
      await nextTick()
      gsap.from(roomRefs.value.filter(Boolean), {
        autoAlpha: 0, x: -20, stagger: 0.06, duration: 0.3, ease: 'power2.out',
      })
    } else {
      createError.value = res.msg || '创建失败'
    }
  } catch (e) {
    createError.value = e?.response?.data?.msg || '创建失败'
  } finally {
    createLoading.value = false
  }
}

function handleLogout() {
  localStorage.removeItem('x-token')
  userInfoStore.clearUserInfo()
  router.push('/login')
}

// --- Admin ---
const showEditRoomDialog = ref(false)
const editRoomReady = ref(false)
const editRoomTarget = ref(null)
const editRoomForm = reactive({ name: '', password: '', description: '', maxMembers: '' })
const editRoomError = ref('')
const editRoomLoading = ref(false)

const showDisbandConfirm = ref(false)
const disbandReady = ref(false)
const disbandTarget = ref('')
const disbandLoading = ref(false)

async function loadAdminData() {
  try {
    const [roomsRes, codesRes] = await Promise.all([RoomApi.adminList(), RoomApi.listInviteCodes()])
    if (roomsRes.code === 0) allRooms.value = roomsRes.data
    if (codesRes.code === 0) inviteCodes.value = codesRes.data
  } catch {}
}

async function handleGenInviteCode() {
  try { await RoomApi.generateInviteCode({ maxMembers: inviteMaxMembers.value }); await loadAdminData() } catch {}
}

async function handleInvalidate(id) {
  try { await RoomApi.invalidateInviteCode(id); await loadAdminData() } catch {}
}

// --- 修改聊天室弹窗 ---
async function openEditRoomDialog(room) {
  editRoomTarget.value = room
  editRoomForm.name = room.name || ''
  editRoomForm.password = ''
  editRoomForm.description = room.description || ''
  editRoomForm.maxMembers = room.maxMembers || ''
  editRoomError.value = ''
  showEditRoomDialog.value = true
  await nextTick()
  editRoomReady.value = true
}

function closeEditRoomDialog() {
  editRoomReady.value = false
  setTimeout(() => { showEditRoomDialog.value = false }, 200)
}

async function submitEditRoom() {
  editRoomError.value = ''
  editRoomLoading.value = true
  try {
    const payload = { roomId: editRoomTarget.value.roomId }
    if (editRoomForm.name) payload.name = editRoomForm.name
    if (editRoomForm.description !== undefined) payload.description = editRoomForm.description
    if (editRoomForm.password) payload.password = editRoomForm.password
    if (editRoomForm.maxMembers) payload.maxMembers = Number(editRoomForm.maxMembers)
    await RoomApi.adminUpdate(payload)
    closeEditRoomDialog()
    await loadAdminData()
  } catch (e) {
    editRoomError.value = e?.response?.data?.msg || '修改失败'
  } finally {
    editRoomLoading.value = false
  }
}

// --- 确认解散弹窗 ---
function openDisbandConfirm(roomId) {
  disbandTarget.value = roomId
  showDisbandConfirm.value = true
  nextTick(() => { disbandReady.value = true })
}

function closeDisbandConfirm() {
  disbandReady.value = false
  setTimeout(() => { showDisbandConfirm.value = false }, 200)
}

async function submitDisband() {
  disbandLoading.value = true
  try {
    await RoomApi.adminDisband({ roomId: disbandTarget.value })
    closeDisbandConfirm()
    await loadAdminData()
    await loadRooms()
  } catch {} finally {
    disbandLoading.value = false
  }
}
</script>

<style lang="less" scoped>
.room-select-container {
  height: 100%;
  width: 100%;
  position: absolute;
  display: flex;
  background: var(--screen-bg-color);

  .operations {
    position: absolute; top: 20px; right: 20px; display: flex; z-index: 10;
  }
  .room-bg {
    width: 100%; height: 100%; display: flex;
    background-image: var(--scrren-grid-bg-color); background-size: 50px 50px;
  }
  .logo {
    margin: 15px; position: absolute; display: flex; height: 60px;
    @media screen and (max-width: 1000px) { height: 40px; }
  }
  .room-content {
    height: 100%; flex: 1; display: flex; justify-content: center; align-items: center; flex-direction: column;
  }
  .room-box {
    width: 600px; min-height: 360px; max-height: 80vh; border-radius: 10px;
    background-image: linear-gradient(130deg, rgba(var(--background-color), 0.3), rgba(var(--background-color), 0.5));
    backdrop-filter: blur(10px); border: rgba(var(--background-color), 0.5) 3px solid;
    padding: 40px 50px; display: flex; flex-direction: column;
    @media screen and (max-width: 1000px) { width: 95%; padding: 30px 20px; }
  }
  .room-list-area { flex: 1; overflow-y: auto; min-height: 80px; max-height: 300px; }
  .room-item {
    display: flex; align-items: center; padding: 10px 12px; border-radius: 8px; margin-bottom: 6px;
    cursor: pointer; transition: background 0.15s;
    &:hover { background: rgba(var(--primary-color), 0.08); }
  }
  .room-action-btn {
    height: 46px; font-size: 16px; font-weight: 600; color: #ffffff;
    background-color: rgb(var(--primary-color)); border-radius: 5px;
    display: flex; justify-content: center; align-items: center;
    cursor: pointer; user-select: none; transition: background 0.2s;
    &:hover { background-color: rgba(var(--primary-color), 0.8); }
    &.create { background-color: #10b981; &:hover { background-color: rgba(16,185,129,0.8); } }
    &.admin { background-color: #f59e0b; &:hover { background-color: rgba(245,158,11,0.8); } }
  }
}

// --- Modal wrapper ---
.modal-wrapper {
  width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center;
}

// --- Dialog card ---
.dialog-card {
  width: 420px;
  background-image: linear-gradient(130deg, rgba(var(--background-color), 0.6), rgba(var(--background-color), 0.85));
  backdrop-filter: blur(16px);
  border: rgba(var(--background-color), 0.6) 2px solid;
  border-radius: 10px;
  padding: 30px 35px;
  will-change: transform, opacity;

  @media screen and (max-width: 500px) { width: 90%; padding: 20px 18px; }
  &.dialog-wide { width: 600px; max-height: 85vh; overflow-y: auto; }
}

// --- Admin ---
.admin-section { margin-bottom: 16px; }
.input-select {
  padding: 8px 12px; border: 1px solid rgba(var(--text-color), 0.15); border-radius: 5px;
  background: rgba(var(--background-color), 0.8); color: rgb(var(--text-color));
  font-size: 14px; outline: none;
}
</style>
