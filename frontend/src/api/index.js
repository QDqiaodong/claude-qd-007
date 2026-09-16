import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const msg = err?.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

export const roomApi = {
  list: (params) => http.get('/rooms', { params }),
  create: (data) => http.post('/rooms', data),
  update: (id, data) => http.put(`/rooms/${id}`, data)
}

export const bedApi = {
  list: (params) => http.get('/beds', { params }),
  create: (data) => http.post('/beds', data),
  update: (id, data) => http.put(`/beds/${id}`, data)
}

export const residentApi = {
  list: (params) => http.get('/residents', { params }),
  create: (data) => http.post('/residents', data),
  update: (id, data) => http.put(`/residents/${id}`, data)
}

export const shiftApi = {
  list: (params) => http.get('/shifts', { params }),
  open: (data) => http.post('/shifts', data),
  advance: (id, action, handoverNote) =>
    http.post(`/shifts/${id}/advance`, null, { params: { action, handoverNote } })
}

export const medicineApi = {
  list: (params) => http.get('/medicines', { params }),
  create: (data) => http.post('/medicines', data),
  update: (id, data) => http.put(`/medicines/${id}`, data)
}

export const medicineIssueApi = {
  list: (params) => http.get('/medicine-issues', { params }),
  create: (data) => http.post('/medicine-issues', data)
}

export const medicalEscortApi = {
  list: (params) => http.get('/medical-escorts', { params }),
  create: (data) => http.post('/medical-escorts', data),
  close: (id, data) => http.post(`/medical-escorts/${id}/close`, data),
  addTakeoutMedicines: (id, data) => http.post(`/medical-escorts/${id}/takeout-medicines`, data)
}

export default http
