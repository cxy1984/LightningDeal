import { defineConfig, presetUno, presetIcons, presetWind, transformerDirectives } from 'unocss'

export default defineConfig({
  presets: [
    presetWind(),
    presetIcons({
      scale: 1.2,
      warn: true,
    }),
  ],
  transformers: [
    transformerDirectives(),
  ],
  shortcuts: {
    // 品牌色
    'brand-gradient': 'bg-gradient-to-r from-yellow-400 via-orange-500 to-red-500',
    'brand-gradient-text': 'bg-gradient-to-r from-yellow-400 via-orange-500 to-red-500 bg-clip-text text-transparent',
    'brand-accent': 'text-red-500',
    'brand-accent-bg': 'bg-red-500',
    'brand-card': 'backdrop-blur-xl bg-white/10 rounded-2xl border border-white/10',
    'brand-card-hover': 'backdrop-blur-xl bg-white/10 rounded-2xl border border-white/10 hover:bg-white/15 hover:border-white/20 transition-all duration-300',
    // 按钮
    'btn-primary': 'px-6 py-3 bg-red-500 hover:bg-red-600 text-white rounded-xl font-semibold transition-all duration-300 shadow-lg shadow-red-500/25 hover:shadow-red-500/40 hover:-translate-y-0.5 active:translate-y-0',
    'btn-ghost': 'px-6 py-3 border border-white/20 hover:border-white/40 text-white/80 hover:text-white rounded-xl font-semibold transition-all duration-300 backdrop-blur-sm',
    // 输入框
    'input-glass': 'w-full px-4 py-3 bg-white/5 border border-white/10 rounded-xl text-white placeholder-white/30 focus:outline-none focus:border-red-500/50 focus:ring-2 focus:ring-red-500/20 transition-all duration-300',
  },
  theme: {
    colors: {
      brand: {
        50: '#fef2f2',
        100: '#fee2e2',
        200: '#fecaca',
        300: '#fca5a5',
        400: '#f87171',
        500: '#ef4444',
        600: '#dc2626',
        700: '#b91c1c',
        800: '#991b1b',
        900: '#7f1d1d',
      },
    },
    fontFamily: {
      display: ['"Helvetica Neue"', 'Helvetica', '"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
      mono: ['"Fira Code"', '"JetBrains Mono"', 'monospace'],
    },
    animation: {
      keyframes: {
        'pulse-glow': '{0%,100%{box-shadow:0 0 20px rgba(239,68,68,0.3)}50%{box-shadow:0 0 40px rgba(239,68,68,0.6)}}',
        'float': '{0%,100%{transform:translateY(0)}50%{transform:translateY(-10px)}}',
      },
    },
  },
  rules: [
    ['animate-pulse-glow', { animation: 'pulse-glow 2s ease-in-out infinite' }],
    ['animate-float', { animation: 'float 3s ease-in-out infinite' }],
  ],
  safelist: [
    'i-ri-flashlight-fill',
    'i-ri-shield-check-fill',
    'i-ri-bar-chart-2-fill',
    'i-ri-cloud-fill',
    'i-ri-wechat-fill',
    'i-ri-alipay-fill',
    'i-ri-github-fill',
  ],
})
