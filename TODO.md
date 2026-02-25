VERSION 0.4.8
---
- Quests are not populating at dawn.
- Add the finance block we discussed before.
- ERROR:

[17:16:53] [Worker-Main-9/WARN] [mojang/YggdrasilMinecraftSessionService]: Couldn't look up profile properties for com.mojang.authlib.GameProfile@7641da1c[id=380df991-f603-344c-a090-369bad2a924a,name=Dev,properties={},legacy=false]
com.mojang.authlib.exceptions.AuthenticationUnavailableException: Cannot contact authentication server
        at com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService.makeRequest(YggdrasilAuthenticationService.java:134) ~[authlib-3.11.49.jar%23158!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService.makeRequest(YggdrasilAuthenticationService.java:106) ~[authlib-3.11.49.jar%23158!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService.fillGameProfile(YggdrasilMinecraftSessionService.java:197) ~[authlib-3.11.49.jar%23158!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService$1.load(YggdrasilMinecraftSessionService.java:64) ~[authlib-3.11.49.jar%23158!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService$1.load(YggdrasilMinecraftSessionService.java:61) ~[authlib-3.11.49.jar%23158!/:?] {}
        at com.google.common.cache.LocalCache$LoadingValueReference.loadFuture(LocalCache.java:3533) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache$Segment.loadSync(LocalCache.java:2282) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache$Segment.lockedGetOrLoad(LocalCache.java:2159) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache$Segment.get(LocalCache.java:2049) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache.get(LocalCache.java:3966) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache.getOrLoad(LocalCache.java:3989) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache$LocalLoadingCache.get(LocalCache.java:4950) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.google.common.cache.LocalCache$LocalLoadingCache.getUnchecked(LocalCache.java:4956) ~[guava-31.0.1-jre.jar%23107!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService.fillProfileProperties(YggdrasilMinecraftSessionService.java:172) ~[authlib-3.11.49.jar%23158!/:?] {}
        at net.minecraft.client.Minecraft.getProfileProperties(Minecraft.java:2405) ~[forge-1.19.2-43.5.0_mapped_official_1.19.2-recomp.jar%23191!/:?] {re:classloading,pl:accesstransformer:B,pl:runtimedistcleaner:A}
        at net.minecraft.client.resources.SkinManager.lambda$registerSkins$4(SkinManager.java:117) ~[forge-1.19.2-43.5.0_mapped_official_1.19.2-recomp.jar%23191!/:?] {re:classloading,pl:runtimedistcleaner:A}
        at java.util.concurrent.ForkJoinTask$RunnableExecuteAction.exec(ForkJoinTask.java:1395) ~[?:?] {}
        at java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:373) ~[?:?] {}
        at java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1182) ~[?:?] {} 
        at java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1655) ~[?:?] {}
        at java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1622) ~[?:?] {}
        at java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:165) ~[?:?] {}     
Caused by: com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $
        at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:226) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:963) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:928) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:877) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:848) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService.makeRequest(YggdrasilAuthenticationService.java:112) ~[authlib-3.11.49.jar%23158!/:?] {}
        ... 21 more
Caused by: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $
        at com.google.gson.stream.JsonReader.beginObject(JsonReader.java:384) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:215) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:963) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:928) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:877) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.google.gson.Gson.fromJson(Gson.java:848) ~[gson-2.8.9.jar%23157!/:?] {}
        at com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService.makeRequest(YggdrasilAuthenticationService.java:112) ~[authlib-3.11.49.jar%23158!/:?] {}
        ... 21 more


