import { AbilityConstant, common, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';

const DOMAIN: number = 0x2600;
const TAG: string = 'AnyiEntryAbility';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    AppStorage.setOrCreate<common.UIAbilityContext>('anyiAbilityContext', this.context);
    hilog.info(DOMAIN, TAG, 'Anyi HarmonyOS client created');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', (error) => {
      if (error.code) {
        hilog.error(DOMAIN, TAG, 'loadContent failed: %{public}s', JSON.stringify(error));
        return;
      }
      hilog.info(DOMAIN, TAG, 'Anyi main page loaded');
    });
  }
}
