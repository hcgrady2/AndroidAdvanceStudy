#### 一、实践步骤
1、工程中使用了相同的图片，加载到了 ImageView 中
2、点击按钮手动生成  Prof 文件，保存到了手机上
3、android studio gradle task buildAlloctrackJar 来生成 jar 包
4、通过 'java -jar DuplicatedBitmapAnalyzer-1.0.jar dump.hprf' 来查看结果


#### 二、dump 文件捕获
##### Heap Dump 概述 ：

Heap Dump 是 Java 进程所使用的内存情况的一次快照。以文件的形式持久化到磁盘中。

Heap Dump 的格式有很多种，每种包含的信息可能都不一样，但总的来说，Heap Dump 文件一般都包含了一个堆中的 Java Objects，class 等基本信息。
同时，当进行一次堆转储时，一般都会触发一次 GC，因此堆转储得到的文件一般都是有效内容。

##### Heap Dump 包含的信息
+ 所有的对象信息

  对象的类信息、字段信息、原生值(int,long等)及引用值。

+  所有的类信息

   类加载器、类名、超类以及静态字段。
  
+  垃圾回收的根对象
    
   根对象是值可以直接被虚拟机触及的对象。
   
+ 线程栈及局部变量

  包含了转储时刻的线程调用栈信息和帧栈中的局部变量信息。
  
##### Heap Dump 获取方式
###### 1、使用 jmap 命令生成 dump 文件
```
jmap -dump:live,format-b,file=d:\dump\heap\heap.hprof <pid>
```

###### 2、使用 jcmd 命令生成 dump 文件
```
jcmd <pid> GC.heap_dump d:\dump\heap.hprof
```

###### 3、使用 JVM 参数获取 dump 文件
+ -XX:+HeapDumpOnOutOfMemoryError

    当发生 oom 是自动 dump

+ -XX:+HeapDumpBeforeFullGC

    当 JVM 执行 FullGc 后执行 dump
    
+ -XX:+HeapDumpOnCtrlBreak

    交互获取 dump，在控制台按下 Ctrol+Break 时，进行堆转储。
    
+ -XX:HeapDumpPath = d:\test.hprof
    
    指定 dump 文件存储路径。
    

###### 4、使用工具获取 dump
如 As 里面的 Profiler 。


###### 5、Debug 手动调用
```
            Debug.dumpHprofData(file.getAbsolutePath());
```


#### 如何分析重复的 Bitmap
可以使用 HAHA 库来进行分析，核心方法为：
```
// 打开hprof文件
final HeapSnapshot heapSnapshot = new HeapSnapshot(hprofFile);
// 获得snapshot
final Snapshot snapshot = heapSnapshot.getSnapshot();
// 获得Bitmap Class
final ClassObj bitmapClass = snapshot.findClass("android.graphics.Bitmap");
// 获得heap, 只需要分析app和default heap即可
Heap heap = snapshot.getHeaps();
// 从heap中获得所有的Bitmap实例
final List<Instance> bitmapInstances = bitmapClass.getHeapInstances(heap.getId());
// 从Bitmap实例中获得buffer数组
ArrayInstance buffer = HahaHelper.fieldValue(((ClassInstance) bitmapInstance).getValues(), "mBuffer")
```

整个思路就是通过 mBuffer 的 哈希值来判断那些图片是重复的。

目前只能在 8.0 一下测试，8.0 以上会分配到 native 上。






