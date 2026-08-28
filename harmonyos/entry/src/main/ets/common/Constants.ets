export const API_BASE_URL: string = 'https://api.anyibj.cn';
export const API_BASE_URLS: Array<string> = [API_BASE_URL];

interface AppColors {
  background: string;
  paper: string;
  ink: string;
  muted: string;
  line: string;
  green: string;
  greenLight: string;
  amber: string;
  amberLight: string;
  danger: string;
  dangerLight: string;
  white: string;
  night: string;
}

export const COLORS: AppColors = {
  background: '#F7F5EF',
  paper: '#FFFEFA',
  ink: '#25231F',
  muted: '#77736A',
  line: '#E5DED0',
  green: '#315F4B',
  greenLight: '#E9F1EA',
  amber: '#C99532',
  amberLight: '#FFF4D9',
  danger: '#B42318',
  dangerLight: '#FFF2EE',
  white: '#FFFFFF',
  night: '#1E2725'
};

export const LEGAL_PRIVACY_URL: string = `${API_BASE_URL}/legal/privacy`;
export const LEGAL_TERMS_URL: string = `${API_BASE_URL}/legal/terms`;
export const LEGAL_AI_URL: string = `${API_BASE_URL}/legal/ai-disclaimer`;
export const ADMIN_URL: string = `${API_BASE_URL}/admin`;
export const DEFAULT_DIGITAL_HUMAN_URL: string = `${API_BASE_URL}/vtuber/`;
export const MAX_IMAGE_BYTES: number = 20 * 1024 * 1024;

export function absoluteAssetUrl(value: string): string {
  if (value.length === 0) {
    return '';
  }
  if (value.startsWith('/')) {
    return `${API_BASE_URL}${value}`;
  }
  if (value.startsWith('http://101.42.1.45/assets/') || value.startsWith('https://101.42.1.45/assets/')) {
    return value.replace(/^https?:\/\/101\.42\.1\.45/, API_BASE_URL);
  }
  if (value.startsWith('http://api.anyibj.cn/assets/')) {
    return value.replace('http://api.anyibj.cn', API_BASE_URL);
  }
  return value;
}
