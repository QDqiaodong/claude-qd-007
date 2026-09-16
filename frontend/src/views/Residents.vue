<template>
  <div>
    <el-card shadow="never">
      <div style="display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-select v-model="query.status" placeholder="按状态" clearable style="width:140px">
          <el-option label="在住" value="在住" />
          <el-option label="已退住" value="已退住" />
          <el-option label="请假外出" value="请假外出" />
        </el-select>
        <el-select v-model="query.careLevel" placeholder="按护理等级" clearable style="width:150px">
          <el-option label="自理" value="自理" />
          <el-option label="半自理" value="半自理" />
          <el-option label="不能自理" value="不能自理" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="姓名或档案号" clearable style="width:180px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openForm()">办入住</el-button>
        <span style="margin-left:auto;color:#909399">
          在住 {{ rows.filter(r => r.status === '在住').length }} 位 / 共 {{ rows.length }} 位
        </span>
      </div>
    </el-card>

    <el-table :data="rows" border stripe size="small" style="margin-top:12px" v-loading="loading">
      <el-table-column prop="code" label="档案号" width="110" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="70" />
      <el-table-column prop="age" label="年龄" width="70" />
      <el-table-column prop="careLevel" label="护理等级" width="110" />
      <el-table-column label="房间" width="120">
        <template #default="{ row }">{{ roomName(row.roomId) }}</template>
      </el-table-column>
      <el-table-column label="床位" width="110">
        <template #default="{ row }">{{ bedName(row.bedId) }}</template>
      </el-table-column>
      <el-table-column prop="checkInDate" label="入住日" width="115" />
      <el-table-column prop="checkOutDate" label="退住日" width="115" />
      <el-table-column prop="familyPhone" label="家属电话" width="130" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === '在住' ? 'success' : row.status === '请假外出' ? 'warning' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">转床</el-button>
          <el-button
            v-if="row.status !== '已退住'"
            link
            type="danger"
            @click="checkOut(row)"
          >办退住</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '转床 / 改资料' : '办入住'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="档案号">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 LA-0007" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio label="女">女</el-radio>
            <el-radio label="男">男</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="form.age" :min="50" :max="120" /></el-form-item>
        <el-form-item label="护理等级">
          <el-select v-model="form.careLevel" style="width:100%">
            <el-option label="自理" value="自理" />
            <el-option label="半自理" value="半自理" />
            <el-option label="不能自理" value="不能自理" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间">
          <el-select v-model="form.roomId" placeholder="选房间" style="width:100%">
            <el-option
              v-for="r in usableRooms"
              :key="r.id"
              :label="`${r.name}（已住 ${living(r.id)}/${r.capacity}）`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="床位">
          <el-select v-model="form.bedId" placeholder="只列这间房里的空床" style="width:100%">
            <el-option v-for="b in freeBeds" :key="b.id" :label="`${b.code}（${b.position}）`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="家属电话"><el-input v-model="form.familyPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bedApi, residentApi, roomApi } from '../api'

const rows = ref([])
const rooms = ref([])
const beds = ref([])
const loading = ref(false)
const query = reactive({ status: '', careLevel: '', keyword: '' })

const visible = ref(false)
const form = reactive({
  id: null, code: '', name: '', gender: '女', age: 80, careLevel: '自理',
  roomId: null, bedId: null, checkInDate: '', familyPhone: ''
})

const usableRooms = computed(() => rooms.value.filter((r) => r.status === '在用'))
const freeBeds = computed(() =>
  beds.value.filter((b) => b.status === '空闲' && (!form.roomId || b.roomId === form.roomId))
)

const roomName = (id) => (id ? rooms.value.find((r) => r.id === id)?.name || `#${id}` : '—')
const bedName = (id) => (id ? beds.value.find((b) => b.id === id)?.code || `#${id}` : '—')
const living = (roomId) => rows.value.filter((r) => r.roomId === roomId && r.status === '在住').length

const load = async () => {
  loading.value = true
  try {
    rows.value = await residentApi.list({
      status: query.status || undefined,
      careLevel: query.careLevel || undefined,
      keyword: query.keyword || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const reloadWarehouse = async () => {
  const [r, b] = await Promise.all([roomApi.list({}), bedApi.list({})])
  rooms.value = r
  beds.value = b
}

const openForm = (row) => {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null, code: '', name: '', gender: '女', age: 80, careLevel: '自理',
      roomId: null, bedId: null, checkInDate: new Date().toISOString().slice(0, 10), familyPhone: ''
    })
  }
  visible.value = true
}

const submit = async () => {
  try {
    if (form.id) {
      await residentApi.update(form.id, {
        name: form.name,
        gender: form.gender,
        age: form.age,
        careLevel: form.careLevel,
        familyPhone: form.familyPhone,
        roomId: form.roomId,
        bedId: form.bedId
      })
    } else {
      await residentApi.create({ ...form })
    }
    ElMessage.success('已保存')
    visible.value = false
    await Promise.all([load(), reloadWarehouse()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const checkOut = async (row) => {
  try {
    await ElMessageBox.confirm(`给 ${row.name} 办退住？床位会空出来。`, '提示')
  } catch {
    return
  }
  try {
    await residentApi.update(row.id, { status: '已退住' })
    ElMessage.success('已办退住')
    await Promise.all([load(), reloadWarehouse()])
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    await reloadWarehouse()
  } catch (e) {
    ElMessage.error(e.message)
  }
  await load()
})
</script>
