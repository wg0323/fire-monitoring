import { http } from '@/utils/request';
import type { UploadResult } from '@/types/api';

export function uploadImage(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<UploadResult>('/upload/image', formData);
}
