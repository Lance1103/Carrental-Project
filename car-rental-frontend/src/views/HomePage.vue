<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAvailableCars } from '@/services/carService'
import AuthService from '@/services/auth.js'

const cars = ref([])
const loading = ref(false)
const error = ref(null)
const router = useRouter()
const authService = AuthService

const currentUser = ref(null)
const isAuthenticated = computed(() => currentUser.value !== null)

const startDate = ref('')
const endDate = ref('')
const filtered = ref(false)

const expandedDescriptions = ref(new Set())

// Get today's date in YYYY-MM-DD format
const today = computed(() => {
  const date = new Date()
  return date.toISOString().split('T')[0]
})

// Validation computed properties
const isStartDateValid = computed(() => {
  if (!startDate.value) return true
  return startDate.value >= today.value
})

const isEndDateValid = computed(() => {
  if (!endDate.value) return true
  if (!startDate.value) return endDate.value >= today.value
  return endDate.value >= startDate.value && endDate.value >= today.value
})

const canSearch = computed(() => {
  return startDate.value &&
      endDate.value &&
      isStartDateValid.value &&
      isEndDateValid.value
})

const initAuth = () => {
  currentUser.value = authService.getCurrentUser()
  console.log('HomePage - Current user:', currentUser.value)
  console.log('HomePage - Is authenticated:', isAuthenticated.value)
}

const filterCars = async () => {
  // Validate dates before searching
  if (!startDate.value || !endDate.value) {
    error.value = 'Please select both start and end dates.'
    return
  }

  if (!isStartDateValid.value) {
    error.value = 'Start date cannot be in the past. Please select a date from today onwards.'
    cars.value = []
    filtered.value = true
    return
  }

  if (!isEndDateValid.value) {
    error.value = 'End date must be after or equal to the start date and cannot be in the past.'
    cars.value = []
    filtered.value = true
    return
  }

  filtered.value = true
  loading.value = true
  error.value = null

  try {
    const fetched = await fetchAvailableCars(startDate.value, endDate.value)
    cars.value = fetched.map(car => ({
      ...car,
      imageUrl: car.image
          ? `data:image/jpeg;base64,${arrayBufferToBase64(car.image)}`
          : `${import.meta.env.VITE_API_URL || 'http://localhost:8082/api/cars'}/${car.carId}/image`
    }))
  } catch (e) {
    console.error(e)
    error.value = 'Failed to fetch cars for the selected dates.'
  } finally {
    loading.value = false
  }
}

// Clear error when dates change
const handleDateChange = () => {
  if (filtered.value) {
    error.value = null
    cars.value = []
    filtered.value = false
  }
}

const goToBooking = (carId) => {
  console.log('Booking car:', carId, 'Authenticated:', isAuthenticated.value)
  router.push({
    name: 'booking',
    params: { carId },
    query: {
      from: startDate.value,
      to: endDate.value
    }
  })
}

const handleLogout = () => {
  authService.logout()
  currentUser.value = null
  console.log('User logged out')
}

const toggleDescription = (carId) => {
  if (expandedDescriptions.value.has(carId)) {
    expandedDescriptions.value.delete(carId)
  } else {
    expandedDescriptions.value.add(carId)
  }
  expandedDescriptions.value = new Set(expandedDescriptions.value)
}

const isDescriptionExpanded = (carId) => {
  return expandedDescriptions.value.has(carId)
}

const isDescriptionLong = (description) => {
  return description && description.length > 100
}

onMounted(() => {
  initAuth()
})

function formatRate(val) {
  if (val == null) return ''
  return Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' }).format(val)
}

function arrayBufferToBase64(buffer) {
  if (!buffer) return ''
  if (typeof buffer === 'string') return buffer
  if (buffer.data && Array.isArray(buffer.data)) buffer = Uint8Array.from(buffer.data).buffer
  let binary = ''
  const bytes = new Uint8Array(buffer)
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i])
  return window.btoa(binary)
}
</script>

<template>
  <div class="min-h-screen flex-col bg-gradient-to-b from-amber-50 via-white to-neutral-100 text-neutral-900">
    <section class="relative flex items-center min-h-[70vh] overflow-hidden">
      <div class="absolute inset-0">
        <img
            src="https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=1650&q=60"
            class="w-full h-full object-cover animate-pan opacity-50"
            alt="hero"
        />
        <div class="absolute inset-0 bg-gradient-to-b from-white/85 via-white/70 to-amber-100/60"></div>
      </div>

      <div class="relative z-10 w-full mx-auto max-w-5xl px-6 text-center">
        <h1 class="text-4xl md:text-6xl font-extrabold tracking-tight leading-tight mb-4 gradient-text-light drop-shadow-lg animate-fade-in">
          Find Your Perfect Ride
        </h1>

        <div class="inline-block backdrop-blur-md bg-white/70 rounded-xl p-4 shadow-lg border border-white/30 animate-fade-in">
          <div class="flex flex-col md:flex-row items-center gap-4">
            <input
                type="date"
                v-model="startDate"
                @change="handleDateChange"
                :min="today"
                :class="{'border-red-500': !isStartDateValid && startDate}"
                class="px-4 py-2 rounded-md border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-amber-500 transition"
            >
            <input
                type="date"
                v-model="endDate"
                @change="handleDateChange"
                :min="startDate || today"
                :class="{'border-red-500': !isEndDateValid && endDate}"
                class="px-4 py-2 rounded-md border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-amber-500 transition"
            >
            <button
                @click="filterCars"
                :disabled="!canSearch || loading"
                :class="{'opacity-50 cursor-not-allowed': !canSearch}"
                class="px-6 py-2 rounded-lg font-semibold bg-gradient-to-r from-amber-400 to-orange-500 text-gray-900 shadow-md hover:scale-[1.01] active:scale-[0.98] transition disabled:hover:scale-100"
            >
              {{ loading ? 'Searching...' : 'Search' }}
            </button>
          </div>

          <!-- Date validation warnings -->
          <div v-if="startDate && !isStartDateValid" class="mt-2 text-sm text-red-600 font-medium">
            ⚠️ Start date must be today or later
          </div>
          <div v-if="endDate && !isEndDateValid" class="mt-2 text-sm text-red-600 font-medium">
            ⚠️ End date must be after start date and not in the past
          </div>
        </div>
      </div>
    </section>

    <main class="relative z-10 flex-1 w-full py-12">
      <div class="mx-auto max-w-6xl px-6">
        <section class="mt-16">
          <h2 class="text-3xl font-bold tracking-tight mb-4">Available Cars</h2>

          <div v-if="!filtered" class="text-center text-neutral-500">
            Please select dates and click "Search" to see available cars.
          </div>

          <div v-else-if="loading" class="text-center text-neutral-500">
            Loading cars...
          </div>

          <div v-else-if="error" class="text-center p-6 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-rose-600 font-medium">{{ error }}</p>
          </div>

          <div v-else-if="cars.length === 0" class="text-center text-neutral-500">
            No cars available for the selected dates.
          </div>

          <div v-else class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 justify-center">
            <div
                v-for="car in cars"
                :key="car.carId"
                class="group relative rounded-xl overflow-hidden border border-amber-200 bg-white shadow-sm hover:shadow-lg transition flex flex-col animate-fade-in"
            >
              <div class="relative h-48 overflow-hidden">
                <img
                    :src="car.imageUrl || 'https://via.placeholder.com/400x200?text=No+Image'"
                    :alt="`${car.make} ${car.model}`"
                    class="w-full h-full object-cover scale-110 group-hover:scale-100 transition-transform duration-700 ease-out"
                />
                
              </div>

              <div class="p-5 flex flex-col flex-1">
                <h3 class="font-semibold text-lg tracking-tight mb-1 text-neutral-900">
                  {{ car.make }} {{ car.model }} ({{ car.year }})
                </h3>

                <div class="relative mb-4">
                  <p
                      :class="[
                      'text-xs text-neutral-500 transition-all duration-300',
                      isDescriptionExpanded(car.carId) ? '' : 'line-clamp-2 min-h-[2.5rem]'
                    ]"
                  >
                    {{ car.description || 'No description available' }}
                  </p>

                  <button
                      v-if="isDescriptionLong(car.description)"
                      @click="toggleDescription(car.carId)"
                      class="text-xs text-amber-600 hover:text-amber-700 font-medium mt-1 focus:outline-none"
                  >
                    {{ isDescriptionExpanded(car.carId) ? 'Show Less' : 'Read More' }}
                  </button>
                </div>

                <div class="mt-auto mb-4">
                  <p class="mb-4 text-2xl font-bold bg-gradient-to-r from-amber-500 via-orange-500 to-rose-500 bg-clip-text text-transparent">
                    {{ formatRate(car.dailyRate) }}
                  </p>

                  <button
                      v-if="isAuthenticated"
                      @click="goToBooking(car.carId)"
                      class="w-full text-white bg-orange-500 px-4 py-2 rounded-lg font-semibold hover:bg-orange-600 transition"
                  >
                    Book Now →
                  </button>
                  <router-link
                      v-else
                      to="/login"
                      class="block w-full text-center text-gray-800 bg-gray-200 px-4 py-2 rounded-lg font-semibold hover:bg-gray-300 transition"
                  >
                    Login to Book →
                  </router-link>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.gradient-text-light {
  background: linear-gradient(90deg, #fbbf24, #fb923c, #f59e0b, #fbbf24);
  -webkit-background-clip: text;
  color: transparent;
  background-size: 300% 100%;
  animation: gradientShift 8s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.animate-fade-in {
  animation: fadeIn 0.8s ease-in-out both;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.animate-pan {
  animation: pan 40s linear infinite;
}

@keyframes pan {
  0% { transform: scale(1.15) translate(0); }
  50% { transform: scale(1.18) translate(-2%, -2%); }
  100% { transform: scale(1.15) translate(0); }
}
</style>