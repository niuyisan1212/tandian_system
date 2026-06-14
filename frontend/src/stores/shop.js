import { defineStore } from 'pinia'
import { getValidShops } from '@/api'

export const useShopStore = defineStore('shop', {
  state: () => ({
    validShops: [],
    loading: false
  }),

  actions: {
    // 获取有效店铺列表
    async fetchValidShops() {
      this.loading = true
      try {
        const res = await getValidShops()
        this.validShops = res.data || []
      } catch (error) {
        console.error('获取有效店铺失败：', error)
        this.validShops = []
      } finally {
        this.loading = false
      }
    }
  }
})
