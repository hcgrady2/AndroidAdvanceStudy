#### 实践步骤
1、工程中使用了相同的图片，加载到了 ImageView 中
2、点击按钮手动生成  Prof 文件，保存到了手机上
3、android studio gradle task buildAlloctrackJar 来生成 jar 包
4、通过 'java -jar DuplicatedBitmapAnalyzer-1.0.jar dump.hprf' 来查看结果