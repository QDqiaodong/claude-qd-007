<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD" placeholder="按日期" clearable style="width:160px" />
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:140px">
          <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="query.nurse" placeholder="护理员" clearable style="width:140px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openShift">排一条</el-button>
        <span style="margin-left:auto;color:#909399">共 {{ rows.length }} 条</span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="shiftNo" label="班次号" width="110" />
      <el-table-column label="房间" width="140">
        <template #default="{ row }">{{ roomName(row.roomId) }}</template>
      </el-table-column>
      <el-table-column prop="shiftDate" label="日期" width="115" />
      <el-table-column prop="period" label="班次" width="80" />
      <el-table-column prop="nurse" label="护理员" width="100" />
      <el-table-column label="时段" width="130">
        <template #default="{ row }">{{ hm(row.startMin) }}-{{ hm(row.endMin) }}</template>
      </el-table-column>
      <el-table-column prop="handoverNote" label="交班注意事项" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <el-button
            v-if="row.status === '待接班' && roomInUse(row.roomId)"
            link
            type="primary"
            @click="advance(row, 'start')"
          >接班</el-button>
          <el-button
            v-if="row.status === '值班中' && roomInUse(row.roomId)"
            link
            type="success"
            @click="openHandover(row)"
          >交班</el-button>
          <el-button
            v-if="row.status === '待接班' || row.status === '值班中'"
            link
            type="danger"
            @click="cancel(row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="排一条护理班次" width="500px">
      <el-form label-width="100px">
        <el-form-item label="房间">
          <el-select v-model="form.roomId" placeholder="只列在用的房间" style="width:100%">
            <el-option
              v-for="r in usableRooms"
              :key="r.id"
              :label="`${r.name}（${r.kind}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.shiftDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="班次">
          <el-radio-group v-model="form.period">
            <el-radio v-for="p in ['早班', '中班', '夜班']" :key="p" :label="p">{{ p }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="护理员"><el-input v-model="form.nurse" /></el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="form.start" start="00:00" step="00:30" end="23:30" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-select v-model="form.end" start="00:00" step="00:30" end="23:30" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">排下去</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handoverVisible" title="交班" width="460px">
      <el-form label-width="110px">
        <el-form-item label="交班注意事项">
          <el-input v-model="handoverNote" type="textarea" :rows="3" placeholder="老人的情况、要特别留意的事" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handoverVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandover">确认交班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roomApi, shiftApi } from '../api'

const statuses = ['待接班', '值班中', '已交班', '已取消']

const rows = ref([])
const rooms = ref([])
const loading = ref(false)
const query = reactive({ date: '', status: '', nurse: '' })

const visible = ref(false)
const form = reactive({
  roomId: null, shiftDate: '', period: '早班', nurse: '', start: '07:00', end: '09:00'
})
const handoverVisible = ref(false)
const handoverNote = ref('')
const handoverId = ref(null)

const usableRooms = computed(() => rooms.value.filter((r) => r.status === '在用'))

const hm = (min) => `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
const toMin = (t) => Number(t.slice(0, 2)) * 60 + Number(t.slice(3, 5))
const roomName = (id) => (id ? rooms.value.find((r) => r.id === id)?.name || `#${id}` : '未选')
const roomInUse = (roomId) => rooms.value.find((r) => r.id === roomId)?.status === '在用'
const tagType = (s) =>
  s === '已交班' ? 'success' : s === '值班中' ? 'warning' : s === '已取消' ? 'info' : ''

const loadRooms = async () => {
  rooms.value = await roomApi.list({})
}

const load = async () => {
  loading.value = true
  try {
    rows.value = await shiftApi.list({
      date: query.date || undefined,
      status: query.status || undefined,
      nurse: query.nurse || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const refresh = () => Promise.all([loadRooms(), load()])

const openShift = () => {
  Object.assign(form, {
    roomId: null,
    shiftDate: new Date().toISOString().slice(0, 10),
    period: '早班', nurse: '', start: '07:00', end: '09:00'
  })
  visible.value = true
}

const submit = async () => {
  if (!form.start || !form.end) {
    ElMessage.warning('请把时段选完整')
    return
  }
  try {
    await shiftApi.open({
      roomId: form.roomId,
      shiftDate: form.shiftDate,
      period: form.period,
      nurse: form.nurse,
      startMin: toMin(form.start),
      endMin: toMin(form.end)
    })
    ElMessage.success('排上了')
    visible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const advance = async (row, action) => {
  try {
    await shiftApi.advance(row.id, action, null)
    ElMessage.success('已更新')
    await refresh()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openHandover = (row) => {
  handoverId.value = row.id
  handoverNote.value = row.handoverNote || ''
  handoverVisible.value = true
}

const submitHandover = async () => {
  try {
    await shiftApi.advance(handoverId.value, 'handover', handoverNote.value)
    ElMessage.success('已交班')
    handoverVisible.value = false
    await refresh()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const cancel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消班次 ${row.shiftNo}？`, '提示')
  } catch {
    return
  }
  advance(row, 'cancel')
}

onMounted(async () => {
  try {
    rooms.value = await roomApi.list({})
  } catch (e) {
    ElMessage.error(e.message)
  }
  query.date = new Date().toISOString().slice(0, 10)
  await load()
})
</script>
