import { photoAccessHelper } from '@kit.MediaLibraryKit';

export async function pickImages(maxSelectNumber: number = 1): Promise<Array<string>> {
  const picker: photoAccessHelper.PhotoViewPicker = new photoAccessHelper.PhotoViewPicker();
  const options: photoAccessHelper.PhotoSelectOptions = new photoAccessHelper.PhotoSelectOptions();
  options.MIMEType = photoAccessHelper.PhotoViewMIMETypes.IMAGE_TYPE;
  options.maxSelectNumber = maxSelectNumber;
  const result: photoAccessHelper.PhotoSelectResult = await picker.select(options);
  return result.photoUris;
}

export async function pickSingleImage(): Promise<string> {
  const values: Array<string> = await pickImages(1);
  return values.length > 0 ? values[0] : '';
}
