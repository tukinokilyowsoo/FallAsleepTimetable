@Entry
@Component
struct Index {
  build() {
    Column({ space: 12 }) {
      Text('FallAsleep 课程表')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor('#1B1B21')
      Text('鸿蒙原生版迁移中')
        .fontSize(16)
        .fontColor('#6B6B75')
      Text('课表、导入、设置页面将按 stage 逐步迁移')
        .fontSize(13)
        .fontColor('#8A8A94')
        .margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#FBF8FF')
  }
}
