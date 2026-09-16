<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px">
          <span>房间</span>
          <el-button size="small" type="success" @click="openRoom()">新增房间</el-button>
          <span style="margin-left:auto;color:#909399">
            在用 {{ rooms.filter(r => r.status === '在用').length }} 间 / 共 {{ rooms.length }} 间
          </span>
        </div>
      </template>
      <el-table :data="rooms" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="房间编号" width="110" />
        <el-table-column prop="name" label="房间" min-width="130" />
        <el-table-column prop="floor" label="楼层" width="80" />
        <el-table-column prop="kind" label="房型" width="100" />
        <el-table-column prop="capacity" label="可住" width="80" />
        <el-table-column label="已住" width="80">
          <template #default="{ row }">
            <span :style="{ color: living(row.id) >= row.capacity ? '#e6a23c' : '' }">{{ living(row.id) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="床位数" width="90">
          <template #default="{ row }">{{ beds.filter(b => b.roomId === row.id).length }} 张</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '在用' ? 'success' : row.status === '维修' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoom(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
          <span>床位</span>
          <el-select v-model="query.roomId" placeholder="按房间" clearable size="small" style="width:160px">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
          <el-select v-model="query.status" placeholder="按状态" clearable size="small" style="width:130px">
            <el-option label="空闲" value="空闲" />
            <el-option label="占用" value="占用" />
            <el-option label="停用" value="停用" />
          </el-select>
          <el-button size="small" type="primary" @click="loadBeds">查询</el-button>
          <el-button size="small" type="success" @click="openBed()">新增床位</el-button>
        </div>
      </template>
      <el-table :data="beds" border stripe size="small" v-loading="loading">
        <el-table-column prop="code" label="床位编号" width="120" />
        <el-table-column label="所属房间" width="150">
          <template #default="{ row }">{{ roomName(row.roomId) }}</template>
        </el-table-column>
        <el-table-column prop="position" label="朝向" width="100" />
        <el-table-column label="住的是谁" min-width="140">
          <template #default="{ row }">{{ holder(row.id) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '空闲' ? 'success' : row.status === '占用' ? 'warning' : 'info'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBed(row)">调整</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="roomVisible" :title="roomForm.id ? '调整房间' : '新增房间'" width="470px">
      <el-form label-width="90px">
        <el-form-item label="编号">
          <el-input v-model="roomForm.code" :disabled="!!roomForm.id" placeholder="如 R-104" />
        </el-form-item>
        <el-form-item label="房间名"><el-input v-model="roomForm.name" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="roomForm.floor" :min="1" /></el-form-item>
        <el-form-item label="房型">
          <el-select v-model="roomForm.kind" style="width:100%">
            <el-option v-for="k in ['单人间', '双人间', '多人间']" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="可住人数"><el-input-number v-model="roomForm.capacity" :min="1" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status" style="width:100%">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
            <el-option label="维修" value="维修" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRoom">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bedVisible" :title="bedForm.id ? '调整床位' : '新增床位'" width="450px">
      <el-form label-width="90px">
        <el-form-item label="床位编号">
          <el-input v-model="bedForm.code" :disabled="!!bedForm.id" placeholder="如 BD-2021" />
        </el-form-item>
        <el-form-item label="所属房间">
          <el-select v-model="bedForm.roomId" style="width:100%">
            <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="朝向">
          <el-select v-model="bedForm.position" style="width:100%">
            <el-option v-for="p in ['靠窗', '靠门', '中间']" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="bedForm.status" style="width:100%">
            <el-option label="空闲" value="空闲" />
            <el-option label="占用" value="占用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bedVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBed">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bedApi, residentApi, roomApi } from '../api'

const rooms = ref([])
const beds = ref([])
const residents = ref([])
const loading = ref(false)
const query = reactive({ roomId: null, status: '' })

const roomVisible = ref(false)
const roomForm = reactive({ id: null, code: '', name: '', floor: 1, kind: '双人间', capacity: 2, status: '在用' })
const bedVisible = ref(false)
const bedForm = reactive({ id: null, code: '', roomId: null, position: '中间', status: '空闲' })

const roomName = (id) => rooms.value.find((r) => r.id === id)?.name || '未归房'
const living = (roomId) =>
  residents.value.filter((r) => r.roomId === roomId && ['在住', '请假外出'].includes(r.status)).length
const holder = (bedId) => {
  const r = residents.value.find((x) => x.bedId === bedId && ['在住', '请假外出'].includes(x.status))
  return r ? `${r.name}（${r.careLevel}${r.status === '请假外出' ? ' · 外出' : ''}）` : '—'
}

const loadRooms = async () => {
  rooms.value = await roomApi.list({})
}

const loadBeds = async () => {
  loading.value = true
  try {
    beds.value = await bedApi.list({
      roomId: query.roomId || undefined,
      status: query.status || undefined
    })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

const openRoom = (row) => {
  if (row) {
    Object.assign(roomForm, row)
  } else {
    Object.assign(roomForm, { id: null, code: '', name: '', floor: 1, kind: '双人间', capacity: 2, status: '在用' })
  }
  roomVisible.value = true
}

const submitRoom = async () => {
  try {
    if (roomForm.id) {
      await roomApi.update(roomForm.id, {
        name: roomForm.name,
        floor: roomForm.floor,
        kind: roomForm.kind,
        capacity: roomForm.capacity,
        status: roomForm.status
      })
    } else {
      await roomApi.create({ ...roomForm })
    }
    ElMessage.success('已保存')
    roomVisible.value = false
    await loadRooms()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const openBed = (row) => {
  if (row) {
    Object.assign(bedForm, row)
  } else {
    Object.assign(bedForm, { id: null, code: '', roomId: null, position: '中间', status: '空闲' })
  }
  bedVisible.value = true
}

const submitBed = async () => {
  try {
    if (bedForm.id) {
      await bedApi.update(bedForm.id, {
        roomId: bedForm.roomId,
        position: bedForm.position,
        status: bedForm.status
      })
    } else {
      await bedApi.create({ ...bedForm })
    }
    ElMessage.success('已保存')
    bedVisible.value = false
    await loadBeds()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(async () => {
  try {
    const [r, s] = await Promise.all([roomApi.list({}), residentApi.list({})])
    rooms.value = r
    residents.value = s
  } catch (e) {
    ElMessage.error(e.message)
  }
  await loadBeds()
})
</script>
