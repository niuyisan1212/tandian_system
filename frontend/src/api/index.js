import request from './request'

/**
 * 店铺相关API
 */

// 搜索店铺信息（根据名称自动填充）
export function searchShopInfo(name, city) {
  return request({
    url: '/shops/search',
    method: 'get',
    params: { name, city }
  })
}

// 获取店铺列表（分页）
export function getShopList(params) {
  return request({
    url: '/shops',
    method: 'get',
    params
  })
}

// 获取所有有效待探店店铺
export function getValidShops() {
  return request({
    url: '/shops/valid',
    method: 'get'
  })
}

// 获取可用人数选项（从数据库获取去重值）
export function getAvailableCountOptions() {
  return request({
    url: '/shops/available-count-options',
    method: 'get'
  })
}

// 获取店铺详情
export function getShopById(id) {
  return request({
    url: `/shops/${id}`,
    method: 'get'
  })
}

// 创建店铺
export function createShop(data) {
  return request({
    url: '/shops',
    method: 'post',
    data
  })
}

// 更新店铺
export function updateShop(id, data) {
  return request({
    url: `/shops/${id}`,
    method: 'put',
    data
  })
}

// 删除店铺
export function deleteShop(id) {
  return request({
    url: `/shops/${id}`,
    method: 'delete'
  })
}

// 标记店铺为已探店
export function markShopVisited(id) {
  return request({
    url: `/shops/${id}/visit`,
    method: 'put'
  })
}

// 导出店铺列表
export function exportShops(params) {
  return request({
    url: '/shops/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 导入店铺列表
export function importShops(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/shops/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 下载导入模板
export function downloadTemplate() {
  return request({
    url: '/shops/template',
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 清单相关API
 */

// 获取清单列表（分页）
export function getListPage(params) {
  return request({
    url: '/lists',
    method: 'get',
    params
  })
}

// 获取清单详情
export function getListById(id) {
  return request({
    url: `/lists/${id}`,
    method: 'get'
  })
}

// 创建清单
export function createList(data) {
  return request({
    url: '/lists',
    method: 'post',
    data
  })
}

// 更新清单
export function updateList(id, data) {
  return request({
    url: `/lists/${id}`,
    method: 'put',
    data
  })
}

// 删除清单
export function deleteList(id) {
  return request({
    url: `/lists/${id}`,
    method: 'delete'
  })
}

// 标记清单为已完成
export function markListCompleted(id) {
  return request({
    url: `/lists/${id}/complete`,
    method: 'put'
  })
}

// 标记清单为已取消
export function markListCancelled(id) {
  return request({
    url: `/lists/${id}/cancel`,
    method: 'put'
  })
}

// 重新规划路线
export function replanRoute(id) {
  return request({
    url: `/lists/${id}/replan`,
    method: 'post'
  })
}

// 选择路线方案
export function selectPlan(id, planType) {
  return request({
    url: `/lists/${id}/select-plan`,
    method: 'put',
    params: { planType }
  })
}

/**
 * 统计相关API
 */

// 获取统计数据
export function getStatistics() {
  return request({
    url: '/statistics/overview',
    method: 'get'
  })
}
