<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.residentId" placeholder="按老人" clearable filterable style="width:190px">
          <el-option
            v-for="r in residents"
            :key="r.id"
            :label="`${r.name}（${r.code}）`"
            :value="r.id"
          />
        </el-select>
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:140px">
          <el-option label="护送中" value="护送中" />
          <el-option label="滞留" value="滞留" />
          <el-option label="已销单" value="已销单" />
        </el-select>
        <el-input v-model="query.hospital" placeholder="医院名称" clearable style="width:190px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">开外出就医护送单</el-button>
        <span style="margin-left:auto;color:#909399">
          未销 {{ rows.filter(r => r.status !== '已销单').length }} 张 / 滞留
          {{ rows.filter(r => r.status === '滞留').length }} 张
        </span>
      </div>
    </el-card>

    <el-table :data="filteredRows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="escortNo" label="护送单号" width="110" />
      <el-table-column label="老人" width="150">
        <template #default="{ row }">
          {{ residentName(row.residentId) }}
          <div style="color:#909399;font-size:12px">{{ roomName(residentRoomId(row.residentId)) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="hospitalName" label="医院" min-width="170" show-overflow-tooltip />
      <el-table-column prop="expectedLeaveAt" label="预计离院" width="170" />
      <el-table-column prop="expectedReturnAt" label="预计返回" width="170" />
      <el-table-column label="护送人" width="140">
        <template #default="{ row }">
          {{ row.escortName }}
          <el-tag size="small" :type="row.escortType === '护理员' ? 'success' : 'warning'">
            {{ row.escortType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="销单" min-width="150">
        <template #default="{ row }">
          <template v-if="row.status === '已销单'">
            {{ row.closeAction }} · {{ row.confirmer }}
          </template>
          <span v-else style="color:#909399">未销单，床位仍占用</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== '已销单'"
            link
            type="primary"
            :disabled="row.status === '滞留'"
            @click="openTakeout(row)"
          >勾外带药</el-button>
          <el-button v-if="row.status !== '已销单'" link type="success" @click="openClose(row)">销单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="开外出就医护送单" width="620px">
      <el-alert
        title="开单成功后档案自动变为请假外出、床位仍算占用；常规在房发药会立即停止。"
        type="warning"
        :closable="false"
        style="margin-bottom:14px"
      />
      <el-form label-width="120px">
        <el-form-item label="老人" required>
          <el-select v-model="form.residentId" filterable placeholder="只列在住且有床位的老人" style="width:100%" @change="onResidentChange">
            <el-option
              v-for="r in creatableResidents"
              :key="r.id"
              :label="`${r.name}（${r.code} · ${r.careLevel} · ${roomName(r.roomId)}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="医院名称" required>
          <el-input v-model="form.hospitalName" placeholder="例如：市第一人民医院" />
        </el-form-item>
        <el-form-item label="预计离院时刻" required>
          <el-date-picker
            v-model="form.expectedLeaveAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择离院时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="预计返回时刻" required>
          <el-date-picker
            v-model="form.expectedReturnAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择返回时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="护送人" required>
          <template v-if="selectedResident && selectedResident.careLevel !== '自理'">
            <el-select v-model="form.escortName" placeholder="必须选择本房此刻值班中的护理员" style="width:100%">
              <el-option v-for="n in currentNurses(selectedResident.roomId)" :key="n" :label="n" :value="n" />
            </el-select>
            <div v-if="currentNurses(selectedResident.roomId).length === 0" style="color:#f56c6c;font-size:12px;margin-top:4px">
              本房此刻没有值班中的班次，单子开不成
            </div>
          </template>
          <template v-else>
            <el-radio-group v-model="familyMode" style="margin-bottom:8px">
              <el-radio :label="false">本房值班护理员</el-radio>
              <el-radio :label="true">家属</el-radio>
            </el-radio-group>
            <el-select v-if="!familyMode" v-model="form.escortName" style="width:100%">
              <el-option v-for="n in selectedResident ? currentNurses(selectedResident.roomId) : []" :key="n" :label="n" :value="n" />
            </el-select>
            <el-input v-else v-model="form.escortName" placeholder="家属姓名" />
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">开单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="takeoutVisible" title="在护送单上勾外带药" width="720px">
      <el-alert
        v-if="takeoutRow"
        :title="`${takeoutRow.escortNo} · ${residentName(takeoutRow.residentId)} · ${takeoutRow.hospitalName}；外带扣库存，在房常规发放不记账。`"
        type="info"
        :closable="false"
        style="margin-bottom:12px"
      />
      <div v-for="(item, index) in takeoutItems" :key="index" style="display:flex;gap:10px;margin-bottom:10px">
        <el-select v-model="item.medicineId" placeholder="药品" style="flex:2">
          <el-option
            v-for="m in medicines"
            :key="m.id"
            :label="`${m.name}（库存 ${m.stock}${m.unit}）`"
            :value="m.id"
          />
        </el-select>
        <el-select v-model="item.doseTime" style="width:90px">
          <el-option label="早" value="早" />
          <el-option label="中" value="中" />
          <el-option label="晚" value="晚" />
        </el-select>
        <el-input-number v-model="item.qty" :min="1" style="width:120px" />
        <el-button link type="danger" @click="takeoutItems.splice(index, 1)">删除</el-button>
      </div>
      <el-button size="small" @click="addTakeoutItem">加一种药</el-button>
      <el-input v-model="takeoutOperator" placeholder="经办人" style="margin-top:12px" />

      <el-divider>本单已勾外带药</el-divider>
      <el-table :data="existingTakeout" size="small" border>
        <el-table-column label="药品" min-width="150">
          <template #default="{ row }">{{ medicineName(row.medicineId) }}</template>
        </el-table-column>
        <el-table-column prop="doseTime" label="剂次" width="70" />
        <el-table-column label="数量" width="100">
          <template #default="{ row }">{{ row.qty }} {{ medicineUnit(row.medicineId) }}</template>
        </el-table-column>
        <el-table-column prop="issueDate" label="日期" width="115" />
      </el-table>

      <template #footer>
        <el-button @click="takeoutVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTakeout">确认外带并扣库存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="销外出就医护送单" width="480px">
      <el-form label-width="110px">
        <el-form-item label="确认结果" required>
          <el-radio-group v-model="closeForm.action">
            <el-radio label="人已回院">人已回院</el-radio>
            <el-radio label="接手护送">接手护送</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="值班确认人" required>
          <el-select v-model="closeForm.confirmer" placeholder="本房此刻值班中的护理员" style="width:100%">
            <el-option
              v-for="n in closeRow ? currentNurses(residentRoomId(closeRow.residentId)) : []"
              :key="n"
              :label="n"
              :value="n"
            />
          </el-select>
          <div v-if="closeRow && currentNurses(residentRoomId(closeRow.residentId)).length === 0"
               style="color:#f56c6c;font-size:12px;margin-top:4px">
            本房此刻没有值班中的护理员，不能销单
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitClose">确认销单，档案回在住</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  medicalEscortApi,
  medicineApi,
  medicineIssueApi,
  residentApi,
  roomApi,
  shiftApi
} from '../api'

const rows = ref([])
const residents = ref([])
const rooms = ref([])
const shifts = ref([])
const medicines = ref([])
const issues = ref([])
const loading = ref(false)
const route = useRoute()
const query = reactive({
  residentId: route.query.residentId ? Number(route.query.residentId) : null,
  status: '',
  hospital: ''
})

const createVisible = ref(false)
const form = reactive({
  residentId: null,
  hospitalName: '',
  expectedLeaveAt: '',
  expectedReturnAt: '',
  escortName: ''
})
const familyMode = ref(false)

const takeoutVisible = ref(false)
const takeoutRow = ref(null)
const takeoutItems = ref([])
const takeoutOperator = ref('')

const closeVisible = ref(false)
const closeRow = ref(null)
const closeForm = reactive({ action: '人已回院', confirmer: '' })

const creatableResidents = computed(() =>
  residents.value.filter((r) => r.status === '在住' && r.bedId && r.roomId)
)
const selectedResident = computed(() =>
  residents.value.find((r) => r.id === form.residentId) || null
)
const filteredRows = computed(() => rows.value.filter((r) =>
  !query.hospital || r.hospitalName.includes(query.hospital)
))
const existingTakeout = computed(() => {
  if (!takeoutRow.value) return []
  return issues.value.filter((i) => i.escortId === takeoutRow.value.id)
})

const residentName = (id) => residents.value.find((r) => r.id === id)?.name || `#${id}`
const residentRoomId = (id) => residents.value.find((r) => r.id === id)?.roomId
const roomName = (roomId) => rooms.value.find((r) => r.id === roomId)?.name || '未排房'
const medicineName = (id) => medicines.value.find((m) => m.id === id)?.name || `#${id}`
const medicineUnit = (id) => medicines.value.find((m) => m.id === id)?.unit || ''
const statusType = (s) => (s === '护送中' ? 'warning' : s === '滞留' ? 'danger' : 'success')

const today = () => new Date().toISOString().slice(0, 10)
const formatLater = (hours) => {
  const d = new Date(Date.now() + hours * 3600000)
  const p = (x) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`
}

const currentNurses = (roomId) => {
  if (!roomId) return []
  return [...new Set(shifts.value
    .filter((s) => s.roomId === roomId && s.status === '值班中')
    .map((s) => s.nurse))]
}

const load = async () => {
  loading.value = true
  try {
    rows.value = await medicalEscortApi.list({
      residentId: query.residentId || undefined,
      status: query.status || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const loadBase = async () => {
  const [residentRows, roomRows, todayShifts, medicineRows, issueRows] = await Promise.all([
    residentApi.list({}),
    roomApi.list({}),
    shiftApi.list({ date: today(), status: '值班中' }),
    medicineApi.list({}),
    medicineIssueApi.list({})
  ])
  residents.value = residentRows
  rooms.value = roomRows
  shifts.value = todayShifts
  medicines.value = medicineRows
  issues.value = issueRows
}

const onResidentChange = () => {
  form.escortName = ''
  familyMode.value = selectedResident.value?.careLevel === '自理'
}

const openCreate = () => {
  Object.assign(form, {
    residentId: null,
    hospitalName: '',
    expectedLeaveAt: formatLater(0.5),
    expectedReturnAt: formatLater(3),
    escortName: ''
  })
  familyMode.value = false
  createVisible.value = true
}

const submitCreate = async () => {
  if (!form.residentId || !form.hospitalName || !form.expectedLeaveAt || !form.expectedReturnAt || !form.escortName) {
    ElMessage.warning('医院名称、预计离院时刻、预计返回时刻和护送人都要填全')
    return
  }
  if (selectedResident.value?.careLevel !== '自理'
      && currentNurses(selectedResident.value.roomId).length === 0) {
    ElMessage.error('本房此刻没有值班中的护理员，护送单开不成')
    return
  }
  if (form.expectedReturnAt <= form.expectedLeaveAt) {
    ElMessage.warning('预计返回时刻必须晚于预计离院时刻')
    return
  }
  try {
    await medicalEscortApi.create({ ...form })
    ElMessage.success('护送单已开，档案已转为请假外出，床位仍占用')
    createVisible.value = false
    await loadBase()
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const addTakeoutItem = () => takeoutItems.value.push({ medicineId: null, doseTime: '早', qty: 1 })
const openTakeout = (row) => {
  takeoutRow.value = row
  takeoutItems.value = [{ medicineId: null, doseTime: '早', qty: 1 }]
  takeoutOperator.value = ''
  takeoutVisible.value = true
}

const submitTakeout = async () => {
  if (takeoutItems.value.some((i) => !i.medicineId)) {
    ElMessage.warning('请把药品选完整')
    return
  }
  try {
    await medicalEscortApi.addTakeoutMedicines(takeoutRow.value.id, {
      operator: takeoutOperator.value,
      items: takeoutItems.value
    })
    ElMessage.success('已勾成外带并扣除库存')
    takeoutVisible.value = false
    await loadBase()
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openClose = (row) => {
  closeRow.value = row
  Object.assign(closeForm, { action: '人已回院', confirmer: '' })
  closeVisible.value = true
}

const submitClose = async () => {
  if (!closeForm.action || !closeForm.confirmer) {
    ElMessage.warning('请选择确认结果和本房此刻值班中的护理员')
    return
  }
  try {
    await medicalEscortApi.close(closeRow.value.id, { ...closeForm })
    ElMessage.success('已销单，档案回到在住')
    closeVisible.value = false
    await loadBase()
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    await loadBase()
  } catch (e) {
    ElMessage.error(e.message)
  }
  await load()
})
</script>
