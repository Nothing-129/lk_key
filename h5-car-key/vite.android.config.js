import { defineConfig } from 'vite';
export default defineConfig({
  build: {
    outDir: '../app/assets/vehicle3d',
    emptyOutDir: true,
    target: 'es2022',
    rollupOptions: { input: 'android.html' },
  },
});
