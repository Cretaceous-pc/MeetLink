import Http from '@/utils/axios'

export default {
  group(roomId) {
    return Http.get('/api/v1/chat-list/group', { roomId })
  },
  privateList() {
    return Http.get('/api/v1/chat-list/list/private')
  },
  create(param) {
    return Http.post('/api/v1/chat-list/create', param)
  },
  read(param) {
    return Http.post('/api/v1/chat-list/read', param)
  },
  delete(param) {
    return Http.post('/api/v1/chat-list/delete', param)
  },
}
