import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus, { ElMessage, ElTableColumn } from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import { authDirective } from './directives/auth';
import './styles/index.css';

const app = createApp(App);

// 全局错误边界：统一记录与提示，不吞掉错误（始终 console.error 保留原始对象），
// 提示做节流避免批量 rejection 造成弹窗风暴
let lastErrorNotice = 0;
function reportError(scope: string, error: unknown): void {
  console.error(`[fm-admin] ${scope}`, error);
  const now = Date.now();
  if (now - lastErrorNotice > 3000) {
    lastErrorNotice = now;
    ElMessage.error('操作未完成，请稍后重试');
  }
}

app.config.errorHandler = (err, _instance, info) => {
  reportError(`组件异常(${info})`, err);
};

window.addEventListener('unhandledrejection', (event) => {
  // 不调用 preventDefault，保留浏览器默认上报，避免静默吞掉错误
  reportError('未处理的 Promise rejection', event.reason);
});

app.use(createPinia());
app.use(router);
app.use(ElementPlus, { locale: zhCn });
// Element Plus 的表格列默认允许用户拖拽调整列宽，全站列表统一关闭
ElTableColumn.setPropsDefaults({ resizable: false });
app.directive('auth', authDirective);
app.mount('#app');
