<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>药品</span>
          <el-select v-model="mq.kind" placeholder="按类别" clearable size="small" style="width:130px">
            <el-option label="处方药" value="处方药" />
            <el-option label="非处方" value="非处方" />
          </el-select>
          <el-select v-model="mq.status" placeholder="按状态" clearable size="small" style="width:120px">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
          <el-input v-model="mq.keyword" placeholder="编号或药名" clearable size="small" style="width:170px" />
          <el-button size="small" type="primary" @click="loadMedicines">查询</el-button>
          <el-button size="small" type="success" @click="openMedicine()">新增药品</el-button>
          <span style="margin-left:auto;color:#909399">
            低于预警线 {{ medicines.filter(m => m.stock <= m.warnStock).length }} 种
          </span>
        </div>
      </template>
      <el-table :data="medicines" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="药品编号" width="120" />
        <el-table-column prop="name" label="药名" min-width="150" />
        <el-table-column prop="kind" label="类别" width="100" />
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <span :style="{ color: row.stock <= row.warnStock ? '#e6a23c' : '' }">
              {{ row.stock }} {{ row.unit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="预警线" width="100">
          <template #default="{ row }">{{ row.warnStock }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openMedicine(row)">调整</el-button>
            <el-button link :type="row.status === '在用' ? 'danger' : 'primary'" @click="toggleMedicine(row)">
              {{ row.status === '在用' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>发放 / 退回流水</span>
          <el-select v-model="iq.residentId" placeholder="按老人" clearable size="small" style="width:170px">
            <el-option v-for="r in residents" :key="r.id" :label="`${r.name}（${r.code}）`" :value="r.id" />
          </el-select>
          <el-select v-model="iq.kind" placeholder="按类型" clearable size="small" style="width:120px">
            <el-option label="发放" value="发放" />
            <el-option label="退回" value="退回" />
          </el-select>
          <el-date-picker v-model="iq.date" type="date" value-format="YYYY-MM-DD" placeholder="按日期" clearable size="small" style="width:150px" />
          <el-button size="small" type="primary" @click="loadIssues">查询</el-button>
          <el-button size="small" type="success" @click="openIssue('发放')">发药</el-button>
          <el-button size="small" @click="openIssue('退回')">退药</el-button>
        </div>
      </template>
      <el-table :data="issues" border stripe size="small" v-loading="loading">
        <el-table-column label="老人" width="140">
          <template #default="{ row }">{{ residentName(row.residentId) }}</template>
        </el-table-column>
        <el-table-column label="药品" min-width="160">
          <template #default="{ row }">{{ medicineName(row.medicineId) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="110">
          <template #default="{ row }">{{ row.qty }} {{ medicineUnit(row.medicineId) }}</template>
        </el-table-column>
        <el-table-column prop="kind" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.kind === '发放' ? 'warning' : 'info'">{{ row.kind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="doseTime" label="剂次" width="80" />
        <el-table-column prop="issueDate" label="日期" width="115" />
        <el-table-column prop="operator" label="经办" width="90" />
      </el-table>
    </el-card>

    <el-dialog v-model="medicineVisible" :title="medicineForm.id ? '调整药品' : '新增药品'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="药品编号">
          <el-input v-model="medicineForm.code" :disabled="!!medicineForm.id" placeholder="如 MD-1007" />
        </el-form-item>
        <el-form-item label="药名"><el-input v-model="medicineForm.name" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="medicineForm.unit" style="width:100%">
            <el-option v-for="u in ['片', '支', '袋', '毫升']" :key="u" :label="u" :value="u" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="medicineForm.kind" style="width:100%">
            <el-option label="非处方" value="非处方" />
            <el-option label="处方药" value="处方药" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存"><el-input-number v-model="medicineForm.stock" :min="0" /></el-form-item>
        <el-form-item label="预警线"><el-input-number v-model="medicineForm.warnStock" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="medicineForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="medicineVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMedicine">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="issueVisible" :title="issueForm.kind === '发放' ? '发药' : '退药'" width="480px">
      <el-form label-width="100px">
        <el-form-item label="老人">
          <el-select v-model="issueForm.residentId" style="width:100%">
            <el-option
              v-for="r in residents"
              :key="r.id"
              :label="`${r.name}（${r.code} · ${r.status}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="药品">
          <el-select v-model="issueForm.medicineId" style="width:100%">
            <el-option
              v-for="m in medicines"
              :key="m.id"
              :label="`${m.name}（库存 ${m.stock} ${m.unit} · ${m.status}）`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="issueForm.qty" :min="1" />
        </el-form-item>
        <el-form-item v-if="issueForm.kind === '发放'" label="剂次">
          <el-radio-group v-model="issueForm.doseTime">
            <el-radio v-for="d in ['早', '中', '晚']" :key="d" :label="d">{{ d }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="issueForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="经办人"><el-input v-model="issueForm.operator" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIssue">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { medicineApi, medicineIssueApi, residentApi } from '../api'

const medicines = ref([])
const issues = ref([])
const residents = ref([])
const loading = ref(false)
const mq = reactive({ kind: '', status: '', keyword: '' })
const iq = reactive({ residentId: null, kind: '', date: '' })

const medicineVisible = ref(false)
const medicineForm = reactive({
  id: null, code: '', name: '', unit: '片', kind: '非处方', stock: 0, warnStock: 0, status: '在用'
})
const issueVisible = ref(false)
const issueForm = reactive({
  residentId: null, medicineId: null, qty: 1, kind: '发放', doseTime: '早', issueDate: '', operator: ''
})

const residentName = (id) => residents.value.find((r) => r.id === id)?.name || `#${id}`
const medicineName = (id) => medicines.value.find((m) => m.id === id)?.name || `#${id}`
const medicineUnit = (id) => medicines.value.find((m) => m.id === id)?.unit || ''

const loadMedicines = async () => {
  loading.value = true
  try {
    medicines.value = await medicineApi.list({
      kind: mq.kind || undefined,
      status: mq.status || undefined,
      keyword: mq.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const loadIssues = async () => {
  loading.value = true
  try {
    issues.value = await medicineIssueApi.list({
      residentId: iq.residentId || undefined,
      kind: iq.kind || undefined,
      date: iq.date || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openMedicine = (row) => {
  if (row) {
    Object.assign(medicineForm, row)
  } else {
    Object.assign(medicineForm, {
      id: null, code: '', name: '', unit: '片', kind: '非处方', stock: 0, warnStock: 0, status: '在用'
    })
  }
  medicineVisible.value = true
}

const submitMedicine = async () => {
  try {
    if (medicineForm.id) {
      await medicineApi.update(medicineForm.id, {
        name: medicineForm.name,
        unit: medicineForm.unit,
        kind: medicineForm.kind,
        stock: medicineForm.stock,
        warnStock: medicineForm.warnStock,
        status: medicineForm.status
      })
    } else {
      await medicineApi.create({ ...medicineForm })
    }
    ElMessage.success('已保存')
    medicineVisible.value = false
    await loadMedicines()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const toggleMedicine = async (row) => {
  try {
    await medicineApi.update(row.id, { status: row.status === '在用' ? '停用' : '在用' })
    ElMessage.success('已更新')
    await loadMedicines()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openIssue = (kind) => {
  Object.assign(issueForm, {
    residentId: null, medicineId: null, qty: 1, kind,
    doseTime: '早', issueDate: new Date().toISOString().slice(0, 10), operator: ''
  })
  issueVisible.value = true
}

const submitIssue = async () => {
  try {
    await medicineIssueApi.create({ ...issueForm })
    ElMessage.success(issueForm.kind === '发放' ? '已发药' : '已退药')
    issueVisible.value = false
    await Promise.all([loadMedicines(), loadIssues()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const [m, r, i] = await Promise.all([
      medicineApi.list({}),
      residentApi.list({}),
      medicineIssueApi.list({})
    ])
    medicines.value = m
    residents.value = r
    issues.value = i
  } catch (e) {
    ElMessage.error(e.message)
  }
})
</script>
