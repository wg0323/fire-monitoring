/**
 * 照片选择与上传：最多 3 张，支持拍照/相册、预览与删除。
 * @date 2026-11-06
 */
import { API_BASE } from '../../config/index';
import { handleUnauthorized } from '../../services/request';
import { getToken } from '../../store/user';
import { FILE_DATA, fileUrl } from '../../utils/file';
import { logError } from '../../utils/log';

interface UploadResult {
  code: number;
  message: string;
  data?: {
    url?: string;
  };
}

/** 上传响应结构校验：JSON.parse 结果不可直接信任 */
const isUploadResult = (value: unknown): value is UploadResult => {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as { code?: unknown; message?: unknown; data?: { url?: unknown } };
  if (typeof candidate.code !== 'number' || typeof candidate.message !== 'string') {
    return false;
  }
  return candidate.data === undefined
    || (typeof candidate.data === 'object' && candidate.data !== null
      && (candidate.data.url === undefined || typeof candidate.data.url === 'string'));
};

const uploadImage = async (filePath: string): Promise<string> => {
  try {
    // 基础库 3.4.10 不传 success/fail 返回 Promise，类型定义仍声明为 UploadTask，故显式断言
    const response = await (wx.uploadFile({
      url: `${API_BASE}/upload/image`,
      filePath,
      name: 'file',
      header: {
        'fm-token': getToken(),
      },
    }) as unknown as Promise<WechatMiniprogram.UploadFileSuccessCallbackResult>);
    let parsed: unknown;
    try {
      parsed = JSON.parse(response.data);
    } catch (error) {
      logError('fm-photo-picker.parse', error);
      throw new Error('上传响应解析失败');
    }
    if (!isUploadResult(parsed)) {
      throw new Error('上传响应格式异常');
    }
    if (response.statusCode === 401 || parsed.code === 401) {
      void handleUnauthorized();
      throw new Error('登录已过期');
    }
    if (parsed.code !== 0 || !parsed.data?.url) {
      throw new Error(parsed.message || '上传失败');
    }
    return parsed.data.url;
  } catch (error) {
    logError('fm-photo-picker.uploadImage', error);
    throw error instanceof Error ? error : new Error('上传失败');
  }
};

Component({
  properties: {
    value: {
      type: Array,
      value: [] as string[],
    },
    max: {
      type: Number,
      value: 3,
    },
    disabled: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    photos: [] as string[],
    ...FILE_DATA,
    uploading: false,
  },

  observers: {
    value(value: string[]) {
      this.setData({ photos: value ?? [] });
    },
  },

  methods: {
    async choose() {
      if (this.data.uploading || this.data.disabled) {
        return;
      }
      const remain = this.properties.max - this.data.photos.length;
      if (remain <= 0) {
        return;
      }
      try {
        const media = await wx.chooseMedia({
          count: remain,
          mediaType: ['image'],
          sourceType: ['camera', 'album'],
          sizeType: ['compressed'],
        });
        this.setData({ uploading: true });
        const urls: string[] = [];
        const files = media.tempFiles;
        // 并发上传限制为 2，避免一次选择多张时挤占连接
        for (let index = 0; index < files.length; index += 2) {
          const batch = files.slice(index, index + 2);
          const uploaded = await Promise.all(batch.map((file) => uploadImage(file.tempFilePath)));
          urls.push(...uploaded);
        }
        const next = [...this.data.photos, ...urls].slice(0, this.properties.max);
        this.setData({ photos: next });
        this.triggerEvent('change', { value: next });
      } catch (error) {
        logError('fm-photo-picker.choose', error);
        const detail = error as { errMsg?: string; message?: string };
        const text = `${detail.errMsg ?? ''} ${detail.message ?? ''}`;
        const message = error instanceof Error ? error.message : '选择照片失败';
        if (!text.includes('cancel')) {
          wx.showToast({ title: message, icon: 'none' });
        }
      } finally {
        this.setData({ uploading: false });
      }
    },

    preview(event: WechatMiniprogram.TouchEvent) {
      const index = Number(event.currentTarget.dataset.index);
      wx.previewImage({
        current: fileUrl(this.data.photos[index]),
        urls: this.data.photos.map(fileUrl),
      });
    },

    remove(event: WechatMiniprogram.TouchEvent) {
      if (this.data.disabled) {
        return;
      }
      const index = Number(event.currentTarget.dataset.index);
      const next = this.data.photos.filter((_, current) => current !== index);
      this.setData({ photos: next });
      this.triggerEvent('change', { value: next });
    },
  },
});
