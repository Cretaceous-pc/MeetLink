import { defineStore } from 'pinia'

export const useGroupStore = defineStore('group', {
  state: () => ({
    name: 'MeetLink在线聊天群',
  }),
  actions: {
    setName(name) {
      this.theme = name
    },
  },
})
