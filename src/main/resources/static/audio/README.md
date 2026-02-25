# 扫码声音文件说明

## 用途
用于企业微信H5页面扫码功能的声音反馈，包括成功和失败两种声音。

## 目录结构
声音文件应存放在以下目录：
```
src/main/resources/static/audio/
```

## 所需文件
需要两个MP3格式的声音文件：

1. **成功声音**：`success.mp3`
   - 用于扫码成功时播放
   - 建议：简短、清脆的提示音

2. **失败声音**：`failure.mp3`
   - 用于扫码失败时播放
   - 建议：稍长、明显的提示音

## 获取声音文件的方法

### 方法1：使用免费音效网站下载
- [Free Sound Effects](https://freesound.org/)
- [Zapsplat](https://www.zapsplat.com/)
- [SoundBible](http://soundbible.com/)
- [Freesound Effects](https://www.freesoundeffects.com/)

### 方法2：使用在线声音生成工具
- [Online Tone Generator](https://www.szynalski.com/tone-generator/)
- [Bfxr](https://www.bfxr.net/)
- [JSFXR](https://sfbgames.itch.io/chiptone)

### 方法3：使用手机或录音设备录制
- 录制简短的提示音
- 使用音频编辑软件（如Audacity）将其转换为MP3格式

## 使用方式

在H5页面中，可以通过以下方式引用声音文件：

```javascript
// 成功声音
const successAudio = new Audio('/audio/success.mp3');
successAudio.play();

// 失败声音
const failureAudio = new Audio('/audio/failure.mp3');
failureAudio.play();
```

## 注意事项
1. 声音文件大小建议控制在50KB以内，以确保快速加载
2. 声音时长建议控制在1秒以内
3. 使用MP3格式，确保兼容性
4. 声音文件命名必须为`success.mp3`和`failure.mp3`

## 部署说明
将下载或生成的声音文件放入上述目录后，重新打包项目即可部署使用。
