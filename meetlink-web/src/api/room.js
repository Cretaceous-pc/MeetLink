import Http from '@/utils/axios'

export default {
  list() {
    return Http.get('/api/v1/room/list')
  },
  join(param) {
    return Http.post('/api/v1/room/join', param)
  },
  create(param) {
    return Http.post('/api/v1/room/create', param)
  },
  leave(param) {
    return Http.post('/api/v1/room/leave', param)
  },
  // --- admin ---
  adminList() {
    return Http.get('/api/v1/admin/rooms')
  },
  adminDisband(param) {
    return Http.post('/api/v1/admin/room/disband', param)
  },
  adminUpdate(param) {
    return Http.post('/api/v1/admin/room/update', param)
  },
  generateInviteCode(param) {
    return Http.post('/api/v1/admin/invite-code', param)
  },
  listInviteCodes() {
    return Http.get('/api/v1/admin/invite-code/list')
  },
  invalidateInviteCode(id) {
    return Http.delete('/api/v1/admin/invite-code/' + id)
  },
}
