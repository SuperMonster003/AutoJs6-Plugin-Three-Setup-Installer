<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-setup-installer-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>يثبت تطبيقات Android ويحدثها ويزيلها بتأكيد النظام أو Shizuku أو Root</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالي

******

### مقدمة

******

يثبت 3-Setup Installer تطبيقات Android ويحدثها ويفحصها ويزيلها عبر مداخل التثبيت في AutoJs6 وطلبات فتح الحزم أو مشاركتها من تطبيقات أخرى. يدعم تأكيد Android المعتاد والتثبيت بصلاحيات Shizuku أو Root. واجهة السكربت وصفحتا الرئيسية والإعدادات المستقلتان ما زالت ضمن الخطط القادمة.

يكتشف AutoJs6 الإضافة عبر خدمة Binder الخاصة بها ويسلمها ملفات الحزم كواصفات ملفات للقراءة فقط. تحلل الإضافة الحزمة وتختار طريقة التفويض وتعرض عند الحاجة مربع حوار التأكيد والتقدم الخاص بها ثم تبلغ عن المراحل والتقدم والنتائج. تعمل العمليات ذات الامتيازات داخل خدمة مستخدم Shizuku أو خدمة root من libsu تتخاطب مباشرة مع مثبت حزم النظام.

******

### الحالة

******

1.0.0: معاينة تطوير P3. تم تنفيذ حوارات التأكيد والتقدم والنتائج والدفعات, والفتح والمشاركة الخارجية, والحذف الاختياري للمصدر, وتأكيد النظام وإشعارات الخدمة الأمامية. واجهة السكربت والرئيسية والإعدادات المستقلة وسجل التثبيت وإعداد المثبت الافتراضي ما زالت مخططة. راجع [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) للتقدم وتغطية الأجهزة. AutoJs6 >= 6.8.0 (5299).

******

### الميزات

******

القدرات المتاحة في معاينة التطوير هذه, مع توضيح الميزات القادمة:

- صيغ الحزم: `.apk` و `.apks` و `.xapk` و `.apkm` و `.apkz` وأرشيفات ZIP التي تحتوي على ملفات APK. تختار الإضافة الحزم المقسمة المناسبة للجهاز, وتتعرف على ملفات `.aab` وتصفها دون تثبيتها.
- طرق التفويض: يستخدم `none` تأكيد Android, ويوفر `shizuku` و`root` عمليات ذات صلاحيات. يختار `auto` المتاح بالترتيب Shizuku ثم Root ثم تأكيد النظام. يمكن اختيار الطريقة في حوار التثبيت.
- يمكن طلب محاولة حذف المصدر بعد نجاح التثبيت. تتطلب العودة لإصدار أقدم وحزم الاختبار وتجاوز حد targetSdk المنخفض (Android 14+) وتحديد المثبت والمستخدمين الآخرين Shizuku أو Root, وتظل خاضعة لقيود Android.
- إزالة صامتة مع خيار الاحتفاظ بالبيانات عبر Shizuku أو Root. وفي غير ذلك يستخدم مربع حوار النظام المعتاد.
- يعرض التأكيد معلومات التطبيق والإصدارين القديم والجديد والتواقيع ومكونات APK القابلة للاختيار. يمكن إلغاء العملية أثناء تقدمها, وتعرض النتائج إجراءات النجاح أو تفاصيل الخطأ القابلة للنسخ. يعرض التثبيت الجماعي حالة كل عنصر.
- افتح ملفات الحزم بالإضافة أو شارك حزمة واحدة أو عدة حزم معها. يمكن إعادة محاولة المصادر الخارجية الفاشلة ما دام URI وصلاحية الوصول متاحين.
- إشعارات تقدم التثبيت في الخدمة الأمامية والإلغاء والنتائج. رفض إذن الإشعارات لا يمنع التثبيت.
- تتبع الحوارات افتراضيا لغة AutoJs6 والوضع الليلي ولون السمة. عند غياب المضيف تستخدم لغة النظام ووضعه الليلي ولونا افتراضيا.
- مخطط: إعداد المثبت الافتراضي, بما يشمل الاختيار بصلاحيات وإرشادات إعدادات النظام عند الحاجة.
- مخطط في P4: واجهة السكربت `installer` (الاسم البديل `$installer`) بصيغ متزامنة و`...Async` وجلسات, وأخطاء `InstallerError` بقيم `code` ثابتة.
- مخطط في P5: صفحتا الرئيسية والإعدادات المستقلتان وسجل التثبيت وإدارة التطبيقات المثبتة.

******

### الاستخدام

******

1. ثبت ملف APK الخاص بالإضافة من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) على جهاز يحتوي على AutoJs6 بالبنية 5299 (6.8.0) أو أحدث.
2. افتح مركز إضافات AutoJs6 وتأكد من التعرف على `3-Setup Installer` ثم فعله.
3. استخدم إجراء التثبيت في AutoJs6 أو اختر 3-Setup Installer عند فتح ملفات الحزم أو مشاركتها. إذا ظهر حوار التأكيد, راجع التطبيق والخيارات قبل التثبيت. جهز تفويض Shizuku أو Root عند اختيار طريقة ذات صلاحيات.

******

### طرق التفويض

******

ما تستطيع كل طريقة فعله وما تحتاج إليه:

- `none`: جلسة PackageInstaller القياسية. يطلب Android من المستخدم تأكيد كل تثبيت, والحزم المقسمة مدعومة, والخيارات ذات الامتيازات غير متاحة.
- `shizuku`: يتطلب تشغيل Shizuku (بالتصحيح اللاسلكي أو ADB أو Root) ومنح الإضافة الإذن. تدعم صلاحيات shell التثبيت والإزالة الصامتين والعمليات للمستخدمين الآخرين.
- `root`: يحتاج إلى مدير Root يمنح `su` للإضافة. يوفر العمليات نفسها التي يوفرها Shizuku عبر خدمة root من libsu. الرجوع إلى إصدار أقدم على البرامج الثابتة العادية (user) لا ينجح إلا مع التطبيقات القابلة للتصحيح, وهذه قاعدة من إطار العمل وليست قيدا من الإضافة.
- **ملاحظة:** عند توفر الصلاحيات, تثبت طلبات المضيف التي تستخدم `interaction: 'auto'` بصمت دون فتح التأكيد تلقائيا. إذا طلب Android التأكيد, يسمح به `auto` ويسجله في `notes`. استخدم `interaction: 'dialog'` لطلب التأكيد قبل التثبيت, أو `interaction: 'silent'` لفشل العملية إن احتاجت تأكيد النظام. ستتبع واجهة السكربت المخططة السلوك الافتراضي نفسه.

******

### بداية سريعة

******

برنامج نصي يثبت بصمت ويحدث مع السماح بالرجوع إلى إصدار أقدم ويراقب جلسة ويزيل تطبيقا (متاح اعتبارا من المرحلة P4):

```js
// Silent installation through the first available authorizer (Shizuku, then Root); the plugin dialog otherwise.
let result = installer.install('/sdcard/Download/app.apk');
console.log(result.ok, result.packageName, result.authorizer);

// Explicit authorizer and options; every failure is an InstallerError with a stable code.
installer.installAsync('/sdcard/Download/old.apk', { authorizer: 'shizuku', allowDowngrade: true, deleteSource: true })
    .then(r => console.log(r.ok ? 'done' : r.error.code))
    .catch(e => console.error(e.code, e.systemMessage));

// Session form with progress events, batch installation, uninstallation and the default installer.
let session = installer.session({ splits: ['/sdcard/base.apk', '/sdcard/split_config.arm64_v8a.apk'] });
session.on('progress', p => console.log(Math.round(p * 100) + '%')).on('complete', r => console.log(r.versionName));
installer.install(['/sdcard/a.apk', '/sdcard/b.xapk']).forEach(r => console.log(r.packageName, r.ok));
installer.uninstall('com.example.app', { keepData: true });
if (!installer.isDefault()) installer.setDefault(true);
```

******

### التوافق

******

حقائق المنصة التي تحدد ما تستطيع الإضافة فعله:

- Android 7.0 (API 24) فأحدث. تسجل نتائج التحقق من الأجهزة والتغطية المتبقية في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- تجاوز حظر targetSdk المنخفض موجود اعتبارا من Android 14 (API 34). على الأنظمة الأقدم يتم تجاهل الخيار مع الإشارة إلى ذلك في النتيجة.
- تقيد بعض أنظمة OEM التطبيق الذي يمكن أن يكون المثبت الافتراضي أو تشترط اسم حزمة مثبت موثوقا (يقبل HyperOS القيمة `com.android.shell`). تبلغ الإضافة عن رد النظام كما هو.

******

### الأسئلة الشائعة

******

- **لماذا ما زال التثبيت يطلب التأكيد?** يستخدم `none` تأكيد النظام دائما. جهز التفويض ثم اختر Shizuku أو Root في حوار التثبيت. قد يظل Android أو نهج الجهاز يطلب تأكيد النظام.
- **هل يمكن تثبيت ملف `.aab`?** لا. Android App Bundle صيغة نشر, فحوله أولا باستخدام bundletool إلى مجموعة `.apks`. تتعرف الإضافة على ملفات `.aab` وتعرض معلومات الحزمة والوحدات.
- **لماذا لم يحذف المصدر?** تجري محاولة الحذف بعد نجاح التثبيت فقط, وقد يرفضها موفر المصدر. يظل التثبيت ناجحا. إذا كان AutoJs6 أو تطبيق مرسل آخر يملك المصدر, يكون ذلك التطبيق مسؤولا عن الحذف.
- **هل يمكن إعادة المحاولة أو الاستئناف?** يمكن إعادة محاولة URI خارجي فاشل ما دام المصدر والوصول متاحين. بعد تحرير المصدر أو صلاحية الوصول, افتح الحزمة مجددا. عند فقدان العملية تعرض الواجهة المستعادة انقطاع الجلسة ولا تعيد التثبيت تلقائيا. تحقق من حالة التطبيق المثبت قبل البدء مجددا.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بإذن التوقيع `org.autojs.permission.PLUGIN`, لذا لا يصل إليها سوى AutoJs6. مدخل "فتح باستخدام" الخارجي لا يقبل إلا ملفات الحزم ولا يشغل أي برنامج نصي أبدا.
- يدعم REQUEST_INSTALL_PACKAGES و REQUEST_DELETE_PACKAGES مربعات حوار التثبيت والإزالة المعتادة, ويتيح QUERY_ALL_PACKAGES للإضافة عرض الإصدار المثبت ومقارنة التوقيعات قبل التحديث.
- يدعم FOREGROUND_SERVICE وFOREGROUND_SERVICE_DATA_SYNC عمل التثبيت في الخلفية, ويتيح POST_NOTIFICATIONS إشعارات التقدم والنتائج. غياب إذن الإشعارات لا يمنع التثبيت.
- لا يستخدم Shizuku و Root إلا للعملية التي تبدأها أنت. الخدمة ذات الامتيازات لا تحتفظ بحالة ولا تبقي أي shell مفتوحا بين العمليات ولا يمكن الوصول إليها من خارج الإضافة.
- تفتح ملفات الحزم للقراءة فقط. لا تجري الإضافة أي طلب شبكة ولا تجمع بيانات وتستثني تخزينها الخاص من النسخ الاحتياطي.

احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون الإضافي والتفاوض على التوافق:

```text
application id: io.github.supermonster003.autojs6.plugin.three.setup.installer
plugin id: three-setup-installer
engine: installer
variant: default
service action: org.autojs.plugin.INSTALLER
service category: installer
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.installer.api.IInstallerPlugin
minimum host build: 5299 (6.8.0)
```

تستجيب `ThreeSetupInstallerPluginService` للإجراء `org.autojs.plugin.INSTALLER` (category `installer`) وتنفذ اتفاقية installer-api الخاصة بالمضيف `org.autojs.plugin.installer.api.IInstallerPlugin` اعتبارا من المرحلة P1. تستجيب `ThreeSetupInstallerPluginInfoService` للإجراء `org.autojs.plugin.INFO` بكائن PluginInfo. يتيح `WakeActivity` للمضيف تنشيط الإضافة.

******

### خارطة الطريق

******

تدار خطط المكون الإضافي وتقدمه كقائمة قابلة للتحقق في ROADMAP.md, منظمة حسب المرحلة مع معايير القبول ومستويات الأدلة. تعبر البنود غير المحددة عن النية لا عن القدرات الحالية; والنقاش عبر Issues موضع ترحيب.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/10/01_

- `تلميح` معاينة تطوير P3. تم تنفيذ حوارات التأكيد والتقدم والنتائج والدفعات, والفتح والمشاركة الخارجية, والحذف الاختياري للمصدر, وتأكيد النظام وإشعارات الخدمة الأمامية. واجهة السكربت والرئيسية والإعدادات المستقلة وسجل التثبيت وإعداد المثبت الافتراضي ما زالت مخططة. راجع [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md) للتقدم وتغطية الأجهزة. AutoJs6 >= 6.8.0 (5299).
- `ميزة` هوية الإضافة `three-setup-installer` (engine `installer`) مع خدمة INFO و Wake Activity وهيكل خدمة `org.autojs.plugin.INSTALLER` لاكتشاف المضيف
- `ميزة` README وتعليمات مركز الإضافات وسجل التغييرات بعشر لغات
- `تحسين` أصبح معرّف الاضافة والمحرك و action / category الخاصة بالخدمة وواصف Binder والحد الأدنى لإصدار المضيف تأتي من ثوابت عقد installer-api في المضيف; تعلن القدرات الإصدار 1 من عقد المثبّت, وتم تحديث الحد الأدنى لبناء المضيف إلى 5299
- `تحسين` تجنب المصادر ذات الوصول العشوائي نسخ الملف بالكامل إلى الذاكرة المؤقتة, مع تخزين التدفقات مؤقتا عند الحاجة. تدعم حزم ZIP المجزأة, وتقتصر ملفات AAB على الفحص, وترفض المصادر التي تغير محتواها.
- `تحسين` لا يستبدل أسلوب التفويض المحدد صراحة بأسلوب آخر. يميز بين الرفض وانتهاء المهلة وعدم التوافق, وتتشارك الطلبات المتزامنة عملية التفويض والاتصالات ذات الامتيازات.
- `تحسين` يدعم محرك التثبيت والتحديث تأكيد النظام أو Shizuku أو Root, مع إمكانية الإلغاء ونتائج تعكس التأكيد الفعلي واستجابة النظام.
- `تحسين` يدعم محرك إلغاء التثبيت تأكيد النظام وShizuku وRoot, مع خيار الاحتفاظ بالبيانات عند استخدام تفويض ذي امتيازات.
- `تحسين` يدعم التثبيت المتتابع لعدة حزم متابعة العمل بعد الفشل أو إلغاء العناصر المتبقية, مع التحقق من المستخدم المستهدف واختياره عند توفر الامتيازات.
- `تحسين` تدعم طلبات خدمة التطبيق المضيف فحص الحزم وتثبيتها وإلغاء تثبيتها والاستعلام عن المستخدمين, مع تأكيد صريح وإلغاء عند خروج الجهة المستدعية وحتى أربع جلسات متزامنة وتنظيف تلقائي.
- `تحسين` تتبع حوارات التثبيت لغة AutoJs6 والوضع الليلي ولون السمة, مع بديل عند غياب المضيف وتخطيطات تدعم النص الكبير والاتجاه من اليمين إلى اليسار.
- `تحسين` أصبح التثبيت في الخلفية يدعم الخدمة الأمامية وإشعارات التقدم والإلغاء والنتائج. رفض إذن الإشعارات لا يمنع التثبيت.
- `تحسين` أضيفت حوارات تأكيد التثبيت والتقدم والنتائج مع معلومات التطبيق واختيار مكونات APK والخيارات ونسخ الأخطاء وحالة كل عنصر. فقدان العملية يظهر انقطاعا دون إعادة تثبيت تلقائية.
- `تحسين` يدعم تأكيد التثبيت إرشادات إذن المصادر غير المعروفة والانقطاع. تعرض الإزالة بصلاحيات معلومات التطبيق وخيار الاحتفاظ بالبيانات قبل التأكيد.
- `تحسين` افتح حزمة واحدة أو عدة حزم أو شاركها, وأعد محاولة المصادر الخارجية ما دام الوصول متاحا, مع محاولة حذف المصدر اختياريا بعد النجاح. رفض الحذف لا يغير نجاح التثبيت.
- `تبعية` إضافة Shizuku API 13.1.5 (`dev.rikka.shizuku:api` و `dev.rikka.shizuku:provider`) لطريقة تفويض Shizuku
- `تبعية` إضافة libsu 6.0.0 (`com.github.topjohnwu.libsu:core` و `service`) لطريقة تفويض Root
- `تبعية` إضافة AndroidHiddenApiBypass 6.1 لواجهات مثبت الحزم المخفية التي تستخدمها الخدمة ذات الامتيازات
- `تبعية` إضافة `common-plugin-api.aar` (وحدة AutoJs6 `plugin-api/common-plugin-api`, بنية المضيف 6.8.0 / 5298, MPL 2.0) كاتفاقية إضافات مشتركة مع قفل التجزئة في `locks/host-api-aars.lock`
- `تبعية` إضافة `package-archive-parser.aar` و `installer-api.aar` (وحدتا AutoJs6 `plugin-api/package-archive-parser` و `plugin-api/installer-api`, بنية المضيف P1 6.8.0 / 5299, MPL 2.0) مع قفل التجزئة في `locks/host-api-aars.lock` إلى جانب `common-plugin-api.aar`
- `تبعية` تحديث محلل الحزم المضمن للتعرف على حاويات ZIP العادية للحزم المقسمة

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يستهدف هذا القسم المطورين الراغبين في بناء المكون الإضافي من المصدر; ويمكن للمستخدمين العاديين ببساطة تثبيت ملف APK الجاهز من صفحة Releases.

بناء APK للتصحيح:

```powershell
.\gradlew.bat :app:assembleDebug
```

تشغيل اختبارات وحدة JVM وبناء APK اختبارات الأجهزة:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

بناء APK الإصدار:

```powershell
.\gradlew.bat :app:assembleRelease
```

جمع ناتج الإصدار وإلحاق الإصدار وملخص CRC32 باسم الملف:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

التحقق من تزامن مصادر التوثيق متعدد اللغات مع النواتج المولدة (يفرض ذلك CI أيضا):

```powershell
py .python\generate_markdown.py --check
```

يتطلب البناء JDK 21 أو أحدث و Android SDK 37; وتدار إصدارات Gradle والمكونات الإضافية مركزيا عبر `version.properties` و `io.github.supermonster003.autojs6-platform-versions`.

******

### التعريب وتوليد التوثيق

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

ملفات JSON اللغوية في `.readme/` و `.changelog/` هي المصدر الوحيد لملف README وتعليمات مركز المكونات الإضافية وسجل التغييرات. عدل دائما مصادر JSON هذه وأعد تشغيل `py .python/generate_markdown.py`; ولا تحرر يدويا نواتج README و `plugin_instruction.md` وسجل التغييرات المولدة أبدا. شغل `py .python/generate_markdown.py --check` للتحقق من جميع النواتج المولدة.

******

### الترخيص

******

كود المشروع مرخص بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/LICENSE). المكونات الخارجية وتراخيصها مدرجة في [إشعارات الجهات الخارجية](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md).

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
- توثيق AutoJs6: https://docs.autojs6.com
- توثيق وحدة المثبت: https://docs.autojs6.com/#/installer
- InstallerX و InstallerX Revived (مرجع معماري, GPL-3.0, دون إعادة استخدام أي كود): https://github.com/iamr0s/InstallerX, https://github.com/wxxsfxyzm/InstallerX-Revived
- إشعارات الجهات الخارجية: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/THIRD_PARTY_NOTICES.md
