<template>
  <div class="flex items-center justify-center min-h-screen bg-gradient-to-b from-amber-50 via-white to-neutral-100">
    <div class="w-full max-w-md p-8 space-y-8 bg-white rounded-xl shadow-lg">

      <div class="flex flex-col items-center space-y-4">
        <router-link to="/">
          <div class="flex items-center gap-3">
            <div class="h-12 w-12 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 shadow-lg flex items-center justify-center font-black text-gray-900 tracking-tighter" aria-label="CarRental logo">
              CR
            </div>
            <h1 class="text-2xl font-bold tracking-tight">
              <span class="bg-gradient-to-r from-amber-500 via-orange-500 to-rose-400 bg-clip-text text-transparent drop-shadow-sm">CarRental</span>
            </h1>
          </div>
        </router-link>
        <h2 class="text-3xl font-bold text-center text-neutral-900">Sign up</h2>
      </div>
      <form @submit.prevent="handleSignup" class="space-y-6">
        <div>
          <label for="firstName" class="sr-only">First Name</label>
          <input id="firstName" name="firstName" type="text" required
                 v-model="firstName"
                 class="relative block w-[375px] px-3 py-2 border border-neutral-300 placeholder-neutral-500 text-neutral-900 rounded-md focus:outline-none focus:ring-amber-500 focus:border-amber-500 focus:z-10 sm:text-sm"

                 placeholder="First Name"
          />
        </div>
        <div>
          <label for="lastName" class="sr-only">Last Name</label>
          <input id="lastName" name="lastName" type="text" required
                 v-model="lastName"
                 class="relative block w-[375px] px-3 py-2 border border-neutral-300 placeholder-neutral-500 text-neutral-900 rounded-md focus:outline-none focus:ring-amber-500 focus:border-amber-500 focus:z-10 sm:text-sm"
                 placeholder="Last Name"
          />
        </div>
        <div>
          <label for="username" class="sr-only">Username</label>
          <input id="username" name="username" type="text" required
                 v-model="username"
                 class="relative block w-[375px] px-3 py-2 border border-neutral-300 placeholder-neutral-500 text-neutral-900 rounded-md focus:outline-none focus:ring-amber-500 focus:border-amber-500 focus:z-10 sm:text-sm"
                 placeholder="Username"
          />
        </div>
        <div>
          <label for="email" class="sr-only">Email address</label>
          <input id="email" name="email" type="email" autocomplete="email" required
                 v-model="email"
                 class="relative block w-[375px] px-3 py-2 border border-neutral-300 placeholder-neutral-500 text-neutral-900 rounded-md focus:outline-none focus:ring-amber-500 focus:border-amber-500 focus:z-10 sm:text-sm"
                 placeholder="Email address"
          />
        </div>
        <div>
          <label for="password" class="sr-only">Password</label>
          <input id="password" name="password" type="password" autocomplete="new-password" required
                 v-model="password"
                 class="relative block w-[375px] px-3 py-2 border border-neutral-300 placeholder-neutral-500 text-neutral-900 rounded-md focus:outline-none focus:ring-amber-500 focus:border-amber-500 focus:z-10 sm:text-sm"
                 placeholder="Password"
          />
        </div>


        <div v-if="error" class="text-center text-red-600 text-sm">
          {{ error }}
        </div>


        <div v-if="success" class="text-center text-green-600 text-sm">
          {{ success }}
        </div>

        <button type="submit" :disabled="loading"
                class="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-amber-600 hover:bg-amber-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-amber-500 disabled:opacity-50 disabled:cursor-not-allowed">
          {{ loading ? 'Creating account...' : 'Sign up' }}
        </button>
      </form>

      <p class="text-center text-sm text-neutral-600">
        Already have an account?
        <router-link to="/login" class="text-amber-600 hover:text-amber-700 font-medium">
          Log in
        </router-link>
      </p>
    </div>
  </div>
</template>


<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import AuthService from '@/services/auth.js';

const firstName = ref('');
const lastName = ref('');
const username = ref('');
const email = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');
const success = ref('');
const router = useRouter();
const authService = AuthService;

const handleSignup = async () => {
  loading.value = true;
  error.value = '';
  success.value = '';

  try {
    const userData = {
      firstName: firstName.value,
      lastName: lastName.value,
      username: username.value,
      email: email.value,
      password: password.value
    };

    await authService.signup(userData);
    success.value = 'Account created successfully! Redirecting to login...';

    setTimeout(() => {
      router.push({ name: 'login' });
    }, 1500);

  } catch (err) {
    console.error('Signup failed:', err);
    error.value = err.response?.status === 400
        ? 'Email already exists'
        : 'Signup failed. Please try again.';
  } finally {
    loading.value = false;
  }
};

</script>