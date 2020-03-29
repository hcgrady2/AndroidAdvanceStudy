### 卡顿现场与卡顿分析
对于卡顿问题，如果平时没怎么遇到的卡顿问题的代码出现了卡顿，可以考虑 synchronized 锁的问题。

#### Java 实现卡顿现场监控
##### 步骤一 ： 获得 Java 线程状态
可以通过 Thread 的 getState 方法获取状态。WAITING，TIME_WAITING,和  BLOCKED 都是需要特别注意的地方。

##### 步骤二： 获得所有线程堆栈
可以通过 Thread.getAllStackTraces() 拿到所有线程的堆栈。（Android 7.0 不会返回主线程堆栈）

确认是否存在主线程和自线程用同一个锁的情况，尤其不要出现主线程等待优先级低的自线程的情况。

缺点： 信息不全。

#### SIGQUIT 信号实现

虽然通过 Java 实现的方式可以监控卡顿现场，但是系统的 ANR 日志包含的信息更加丰富，我们可以直接利用这个信息。

如：
// 线程名称; 优先级; 线程id; 线程状态
```
"main" prio=5 tid=1 Suspended
  // 线程组;  线程suspend计数; 线程debug suspend计数; 
  | group="main" sCount=1 dsCount=0 obj=0x74746000 self=0xf4827400
  // 线程native id; 进程优先级; 调度者优先级;
  | sysTid=28661 nice=-4 cgrp=default sched=0/0 handle=0xf72cbbec
  // native线程状态; 调度者状态; 用户时间utime; 系统时间stime; 调度的CPU
  | state=D schedstat=( 3137222937 94427228 5819 ) utm=218 stm=95 core=2 HZ=100
  // stack相关信息
  | stack=0xff717000-0xff719000 stackSize=8MB
```

关于 Native 线程状态

上面的 Suspended 这个状态其实 Java 中没有，而是 Native 里面的一个状态。

![](./images/img.png)

获取 ANR 日志

既然要获取类似 ANR 信息的卡顿日志，就需要利用系统 ANR 的生成机制：

1. 监控主线程卡顿时，主动向系统发送 SIGQUIT 信号

2. 等待 /data/anr/traces.txt 文件生存

3. 文件生成之后进行上报

生存的 anr 如下：
```
  // 堆栈相关信息
  at android.content.res.AssetManager.open(AssetManager.java:311)
  - waiting to lock <0x41ddc798> (android.content.res.AssetManager) held by tid=66 (BackgroundHandler)
  at android.content.res.AssetManager.open(AssetManager.java:289)

```


可以清晰的看到主线程的锁被 BackgroundHandler 线程持有。

缺点：

• 高版本系统中，没有权限读取 /data/anr/traces.txt 文件。

• 获取所有线程堆栈和各种信息非常耗时，可能进一步加剧卡顿。

#### Hook 实现

因为通过 SIGQUIT 的方法可能有限制或者更加耗时。因此需要换一种获取线程堆栈的方法。

• 通过 libart.so ，dlsys 调用 ThreadList::ForEach 方法，拿到所有 Native 线程对象。

• 遍历线程对象列表，调用 Thread::DumpState 方法拿到堆栈信息。

同时，为了将对主进程的影响降到最低，可以通过 fork 子进程来进行。（可能通过 /proc/self 方式获取一些信息会失败， 不过这些信息可以通过指定 /proc/父进程 id/ 来获取）。
```
"main" prio=7 tid=1 Native
  | group="" sCount=0 dsCount=0 obj=0x74e99000 self=0xb8811080
  | sysTid=23023 nice=-4 cgrp=default sched=0/0 handle=0xb6fccbec
  | state=? schedstat=( 0 0 0 ) utm=0 stm=0 core=0 HZ=100
  | stack=0xbe4dd000-0xbe4df000 stackSize=8MB
  | held mutexes=
```


其他相关的现场信息收集

• CPU 使用率和调度信息

• 内存相关信息

• I/O 和网络相关

### 卡顿分析

卡顿问题应该关注的指标：

1、卡顿率

UV 卡顿率 = 发生过卡顿 UV / 开启卡顿采集 UV

PV 卡顿率 = 发生过卡顿 PV / 启动采集 PV

2、卡顿树

![](./images/two.png)


按照相同堆栈出现的比例来聚合。这样我们从一棵树上面，就可以看到哪些堆栈出现的卡顿问题最多，它下面又存在的哪些分支。

### Android 线程的创建过程
[Android 线程的创建过程](http://gityuan.com/2016/09/24/android-thread/)




### Chapter06-plus

该项目展示了如何使用 PLTHook 技术来获取线程创建的堆栈

运行环境

AndroidStudio3.2
NDK16~19
支持 `x86` `armeabi-v7a`

说明
====

运行项目后点击`开启 Thread Hook`按钮，然后点击`新建 Thread`按钮。在Logcat 日志中查看到捕获的日志，类似如下：

```
com.dodola.thread.MainActivity$2.onClick(MainActivity.java:33)
    android.view.View.performClick(View.java:5637)
    android.view.View$PerformClick.run(View.java:22429)
    android.os.Handler.handleCallback(Handler.java:751)
    android.os.Handler.dispatchMessage(Handler.java:95)
    android.os.Looper.loop(Looper.java:154)
    android.app.ActivityThread.main(ActivityThread.java:6121)
    java.lang.reflect.Method.invoke(Native Method)
    com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:889)
    com.android.internal.os.ZygoteInit.main(ZygoteInit.java:779)
```


实现步骤
====

### 寻找Hook点

我们来先线程的启动流程，可以参考这篇文章[Android线程的创建过程](https://www.jianshu.com/p/a26d11502ec8)

[java_lang_Thread.cc](http://androidxref.com/9.0.0_r3/xref/art/runtime/native/java_lang_Thread.cc#43):Thread_nativeCreate
```
static void Thread_nativeCreate(JNIEnv* env, jclass, jobject java_thread, jlong stack_size, jboolean daemon) {
  Thread::CreateNativeThread(env, java_thread, stack_size, daemon == JNI_TRUE);
}
```

[thread.cc](http://androidxref.com/9.0.0_r3/xref/art/runtime/thread.cc) 中的CreateNativeThread函数

```
void Thread::CreateNativeThread(JNIEnv* env, jobject java_peer, size_t stack_size, bool is_daemon) {
    ...
    pthread_create_result = pthread_create(&new_pthread,
                                             &attr,
                                             Thread::CreateCallback,
                                             child_thread);
    ...
}
```

整个流程就非常简单了，我们可以使用inline hook函数Thread_nativeCreate或者CreateNativeThread。

不过考虑到inline hook的兼容性，我们更希望使用got hook或者plt hook。

pthread_create就是一个非常好的点，我们可以利用它来做文章

### 查找Hook的So
上面Thread_nativeCreate、CreateNativeThread和pthread_create函数分别编译在哪个library中呢？

很简单，我们看看编译脚本[Android.bp](http://androidxref.com/9.0.0_r3/xref/art/runtime/Android.bp)就知道了。

```
art_cc_library {
   name: "libart",
   defaults: ["libart_defaults"],
}

cc_defaults {
   name: "libart_defaults",
   defaults: ["art_defaults"],
   host_supported: true,
   srcs: [
    thread.cc",
   ]
}
```

可以看到是在"libart.so"中，而pthread_create熟悉的人都知道它是在"libc.so"中的。

### 查找Hook函数的符号

C++ 的函数名会 Name Mangling，我们需要看看导出符号。

```
readelf -a libart.so

```

pthread_create函数的确是在libc.so中，而且因为c编译的不需要deMangling

```
001048a0  0007fc16 R_ARM_JUMP_SLOT   00000000   pthread_create@LIBC
```

### 真正实现

剩下的实现就非常简单了，如果你想监控其他so库的pthread_create。

profilo中也有一种做法是把目前已经加载的所有so都统一hook了，考虑到性能问题，我们并没有这么做，而且只hook指定的so.

```
hook_plt_method("libart.so", "pthread_create", (hook_func) &pthread_create_hook);

```

而pthread_create的参数直接查看[pthread.h](http://androidxref.com/9.0.0_r3/xref/bionic/libc/include/pthread.h)就可以了。

```
int pthread_create(pthread_t* __pthread_ptr, pthread_attr_t const* __attr, void* (*__start_routine)(void*), void*);
```

获取堆栈是在native反射Java的方法

```
jstring java_stack = static_cast<jstring>(jniEnv->CallStaticObjectMethod(kJavaClass, kMethodGetStack));
```

可以看到整个流程的确是so easy.