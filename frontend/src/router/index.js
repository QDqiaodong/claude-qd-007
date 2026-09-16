import { createRouter, createWebHistory } from 'vue-router'
import Rooms from '../views/Rooms.vue'
import Residents from '../views/Residents.vue'
import Shifts from '../views/Shifts.vue'
import Medicines from '../views/Medicines.vue'
import MedicalEscorts from '../views/MedicalEscorts.vue'

const routes = [
  { path: '/', redirect: '/rooms' },
  { path: '/rooms', component: Rooms, meta: { title: '房间与床位' } },
  { path: '/residents', component: Residents, meta: { title: '老人档案' } },
  { path: '/shifts', component: Shifts, meta: { title: '护理班次' } },
  { path: '/medicines', component: Medicines, meta: { title: '药品与发放' } },
  { path: '/medical-escorts', component: MedicalEscorts, meta: { title: '外出就医护送' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
