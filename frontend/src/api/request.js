import axios from 'axios'
import { message } from 'ant-design-vue'

// 创建axios实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 如果是 blob 类型（文件下载），直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }
    
    const res = response.data
    
    // 根据code判断请求是否成功
    if (res.code === 200) {
      return res
    } else {
      message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('响应错误：', error)
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
