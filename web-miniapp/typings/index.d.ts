/// <reference path="../node_modules/miniprogram-api-typings/types/index.d.ts" />

interface IAppOption {
  globalData: {
    user: import('../miniprogram/store/user').UserInfo | null;
  };
}
