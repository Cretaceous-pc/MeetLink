import EventBus from '@/utils/eventBus.js'

let ws = null
let heartTimer = null
let timer = null
let lockReconnect = false
let token = null
let currentRoomId = null
const reconnectCountMax = 200
let reconnectCount = 0
let isConnect = false

function response(event) {
  if (event.type !== 'message') {
    onCloseHandler()
    return
  }
  let wsContent
  try {
    wsContent = JSON.parse(event.data)
  } catch {
    onCloseHandler()
    return
  }
  if (wsContent.type) {
    // 解散房间通知
    if (wsContent.type === 'disband') {
      EventBus.emit('on-room-disband', wsContent.content)
      return
    }
    if (wsContent.data && wsContent.data.code === -1) {
      onCloseHandler()
    } else {
      switch (wsContent.type) {
        case 'msg': {
          EventBus.emit('on-receive-msg', wsContent.content)
          break
        }
        case 'notify': {
          if (wsContent.content?.content) {
            try {
              wsContent.content.content = JSON.parse(wsContent.content.content)
            } catch {}
          }
          EventBus.emit('on-receive-notify', wsContent.content)
          break
        }
        case 'video': {
          EventBus.emit('on-receive-video', wsContent.content)
          break
        }
        case 'file': {
          EventBus.emit('on-receive-file', wsContent.content)
          break
        }
      }
    }
  } else {
    onCloseHandler()
  }
}

function connect(tokenStr, roomId) {
  if (isConnect && ws && currentRoomId === roomId) return
  // 如果房间变了，先断开
  if (ws && currentRoomId !== roomId) {
    disConnect()
  }
  isConnect = true
  token = tokenStr
  currentRoomId = roomId
  try {
    const wsIp = import.meta.env.VITE_WS_URL
    let url = wsIp + '/ws?x-token=' + token
    if (roomId) {
      url += '&roomId=' + roomId
    }
    ws = new WebSocket(url)

    ws.onopen = () => {
      console.log('Connected to server, room=' + roomId)
      clearTimer()
      sendHeartPack()
    }

    ws.onmessage = response
    ws.onclose = onCloseHandler
    ws.onerror = onCloseHandler
  } catch {
    onCloseHandler()
  }
}

function send(msg) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(msg)
}

const sendHeartPack = () => {
  heartTimer = setInterval(() => {
    send('heart')
  }, 9900)
}

const onCloseHandler = () => {
  clearHeartPackTimer()
  if (ws) {
    ws.close()
    ws = null
  }
  isConnect = false
  if (lockReconnect) return
  lockReconnect = true
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  if (reconnectCount >= reconnectCountMax) {
    reconnectCount = 0
    return
  }
  if (token) {
    timer = setTimeout(() => {
      connect(token, currentRoomId)
      reconnectCount++
      lockReconnect = false
    }, 5000)
  }
}

const clearHeartPackTimer = () => {
  console.log('Closing connection')
  if (heartTimer) {
    clearInterval(heartTimer)
    heartTimer = null
  }
}

const clearTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const disConnect = () => {
  clearHeartPackTimer()
  token = null
  currentRoomId = null
  if (ws) {
    ws.close()
    ws = null
  }
  isConnect = false
}

export default { connect, disConnect }
