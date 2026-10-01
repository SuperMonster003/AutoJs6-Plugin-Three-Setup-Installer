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

يثبت 3-Setup Installer تطبيقات Android ويحدثها ويفحصها ويزيلها من شاشته الرئيسية المستقلة, أو عبر مداخل AutoJs6 وبرامجه النصية, أو طلبات فتح الحزم ومشاركتها من التطبيقات الخارجية. يدعم تأكيد Android والعمليات ذات الصلاحيات عبر Shizuku أو Root.

يكتشف AutoJs6 الإضافة عبر خدمة Binder الخاصة بها ويسلمها ملفات الحزم كواصفات ملفات للقراءة فقط. تحلل الإضافة الحزمة وتختار طريقة التفويض وتعرض عند الحاجة مربع حوار التأكيد والتقدم الخاص بها ثم تبلغ عن المراحل والتقدم والنتائج. تعمل العمليات ذات الامتيازات داخل خدمة مستخدم Shizuku أو خدمة root من libsu تتخاطب مباشرة مع مثبت حزم النظام.

******

### الحالة

******

يوثق 1.0.0 ميزات التثبيت وإدارة التطبيقات والبرامج النصية المنفذة أدناه. لم يكتمل بعد النشر الرسمي في GitHub Releases والإدراج في مركز الإضافات. يتطلب التكامل AutoJs6 >= 6.8.0 (5299), وتتطلب واجهة البرامج النصية `installer` البناء 5300 أو أحدث. تسجل تغطية الأجهزة والتحققات المتبقية في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).

******

### الميزات

******

الميزات المنفذة في 1.0.0:

- صيغ الحزم: `.apk` و `.apks` و `.xapk` و `.apkm` و `.apkz` وأرشيفات ZIP التي تحتوي على ملفات APK. تختار الإضافة الحزم المقسمة المناسبة للجهاز, وتتعرف على ملفات `.aab` وتصفها دون تثبيتها.
- طرق الصلاحيات: تستخدم `none` تأكيد Android, وتوفر `shizuku` و`root` عمليات ذات صلاحيات. يختار `auto` افتراضيا Shizuku ثم Root ثم تأكيد النظام بحسب التوفر. تتيح الإعدادات تغيير الترتيب وتفعيل الطرق, ولا يستبدل الاختيار الصريح بطريقة أخرى تلقائيا.
- يمكن طلب محاولة حذف المصدر بعد نجاح التثبيت. تتطلب العودة لإصدار أقدم وحزم الاختبار وتجاوز حد targetSdk المنخفض (Android 14+) وتحديد المثبت والمستخدمين الآخرين Shizuku أو Root, وتظل خاضعة لقيود Android.
- تتيح إدارة التطبيقات المثبتة البحث بالاسم أو اسم الحزمة والترتيب حسب الاسم أو وقت التثبيت أو التحديث وإظهار تطبيقات النظام. يمكنك فتح التطبيق أو معلوماته في النظام, أو مراجعة إزالته وتأكيدها. يمكن لـ Shizuku أو Root الإزالة بعد التأكيد دون طلب نظام إضافي مع الاحتفاظ بالبيانات اختياريا, وتستخدم الحالات الأخرى تأكيد Android.
- يعرض التأكيد معلومات التطبيق والإصدارين القديم والجديد والتواقيع ومكونات APK القابلة للاختيار. يمكن إلغاء العملية أثناء تقدمها, وتعرض النتائج إجراءات النجاح أو تفاصيل الخطأ القابلة للنسخ. يعرض التثبيت الجماعي حالة كل عنصر.
- افتح أو شارك حزمة واحدة أو عدة حزم, بما فيها ملفات APKS التي يشاركها MT Manager. تدخل الحزم المتعددة في طابور متتابع. يمكن إعادة محاولة العناصر الخارجية الفاشلة ما دام URI وإذن الوصول متاحين.
- إشعارات تقدم التثبيت في الخدمة الأمامية والإلغاء والنتائج. رفض إذن الإشعارات لا يمنع التثبيت.
- تشمل إعدادات المظهر اللغة والوضع الليلي ولون السمة وأيقونة المشغل. تتبع العناصر الثلاثة الأولى AutoJs6 افتراضيا وتقبل تجاوزات محلية; وعند غياب المضيف تستخدم لغة النظام ووضعه واللون الافتراضي. تتوفر للأيقونة أوضاع فاتح وداكن وتلقائي وشفاف; يتبع التلقائي النظام ويتأثر بالتخزين المؤقت وأقنعة المشغل.
- تفتح بطاقة الحالة الرئيسية والإعدادات صفحة المثبت الافتراضي نفسها, مع تعيينه أو إلغائه بصلاحيات وإرشادات إلى إعدادات النظام عند غياب الصلاحيات. قد تمنع سياسات الشركة المصنعة التغيير أو تتطلب مسح المعالج السابق. تبقى `installer.isDefault` و`installer.setDefault` و`setDefaultAsync` متاحة للبرامج النصية, وتعكس النتائج استجابة الجهاز.
- توفر واجهة السكربت `installer` (الاسم البديل `$installer`) صيغ التزامن و`...Async` والجلسات للتثبيت الفردي والدفعات والحزم المقسمة, والإزالة والفحص والاستعلام عن طرق التفويض والمستخدمين وتعيين المثبت الافتراضي. الأخطاء من نوع `InstallerError` بقيم `code` ثابتة (يتطلب AutoJs6 >= 6.8.0 (5300)).
- تعرض الشاشة الرئيسية المستقلة توفر Shizuku/Root وصلاحياتهما والمثبت الافتراضي والمهام النشطة وعمليات التثبيت الأخيرة. اختر عدة حزم من منتقي ملفات النظام لتثبيتها بالتتابع, أو المتابعة بعد فشل عنصر, أو إلغاء العناصر المتبقية.
- يحتفظ سجل التثبيت الخاص بما يصل إلى 200 عنصر تشمل الحزمة والاسم والإصدار القديم/الجديد والنتيجة والوقت والمصدر (المضيف/برنامج نصي/خارجي/الرئيسية) وطريقة الصلاحيات وتفاصيل الفشل. يمكنك حذف سجل فردي أو مسح السجل دون إزالة التطبيقات أو حذف الملفات المصدرية. بعد انتهاء العملية تصبح العناصر غير المكتملة ملغاة ولا تستأنف تلقائيا.
- تحفظ الإعدادات ترتيب طرق الصلاحيات وتفعيلها وخيارات التثبيت وتفضيلات إشعارات التقدم. يستخدم التثبيت من الرئيسية والتطبيقات الخارجية `dialog` افتراضيا, وتطبق خيارات `auto` أو `silent` المحفوظة صراحة. تحتفظ طلبات المضيف/البرامج النصية بخياراتها الصريحة وتبقى القيمة الافتراضية لواجهة البرامج النصية `auto`. لا تحفظ الاختيارات إلا بعد التأكيد.
- تتوفر صفحة حول وسجل الإصدارات المدمج بعشر لغات من الإعدادات. يستخدم فحص التحديث اليدوي واجهة GitHub Releases الخاصة بالملحق بفاصل 12 ساعة مع نتائج مخزنة مؤقتا وإدارة الإصدارات المتجاهلة. تفتح صفحات الإصدارات في المتصفح, ولا تنزل التحديثات أو تثبت تلقائيا.

******

### الاستخدام

******

1. على Android 7.0 أو أحدث, ثبت APK الرسمي من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) بعد نشره, أو استخدم معالج التثبيت في مركز إضافات AutoJs6 بعد إدراجه في الفهرس الرسمي. قبل النشر, استخدم للاختبار بناء يقدمه المشرف أو ابن من المصدر. تفتح أيقونة المشغل الشاشة الرئيسية المستقلة.
2. تحقق في الرئيسية من الصلاحيات والمثبت الافتراضي, ثم استخدم زر الإضافة لاختيار حزمة واحدة أو عدة حزم. راجع حوار التثبيت قبل التأكيد وتابع التقدم والسجل الأخير من الرئيسية.
3. للتكامل مع AutoJs6, استخدم البناء 5299 (6.8.0) أو أحدث وفعل `3-Setup Installer` في مركز الملحقات. تتطلب واجهة البرامج النصية البناء 5300 أو أحدث.
4. استخدم إجراء التثبيت في AutoJs6 أو اختر 3-Setup Installer عند فتح ملفات الحزم أو مشاركتها. إذا ظهر حوار التأكيد, راجع التطبيق والخيارات قبل التثبيت. جهز تفويض Shizuku أو Root عند اختيار طريقة ذات صلاحيات.
5. افتح التطبيقات المثبتة والإعدادات من قائمة الرئيسية. راجع افتراضيات التثبيت المحلية والمظهر والأيقونة والإشعارات; توجد صفحة حول وسجل الإصدارات وفحص التحديث اليدوي في الإعدادات.

******

### طرق التفويض

******

ما تستطيع كل طريقة فعله وما تحتاج إليه:

- `none`: جلسة PackageInstaller القياسية. يطلب Android من المستخدم تأكيد كل تثبيت, والحزم المقسمة مدعومة, والخيارات ذات الامتيازات غير متاحة.
- `shizuku`: يتطلب تشغيل Shizuku (بالتصحيح اللاسلكي أو ADB أو Root) ومنح 3-Setup Installer إذنا مستقلا. منح الإذن إلى AutoJs6 لا يفوض هذه الإضافة. تستخدم عمليات التثبيت والإزالة والمستخدمين الآخرين صلاحيات خدمة Shizuku الجارية.
- `root`: يتطلب جهازا بصلاحيات Root ومدير Root يمنح `su` إلى 3-Setup Installer. يوفر عبر libsu التثبيت والإزالة وعمليات المستخدمين والمثبت الافتراضي بصلاحيات مرتفعة. يظل السماح بكل عملية خاضعا لسياسات Android وROM.
- **ملاحظة:** تستخدم البرامج النصية `interaction: 'auto'` افتراضيا وتثبت بصمت عند توفر الصلاحيات. تستخدم إجراءات التثبيت في واجهة المضيف `dialog`. إذا طلب Android التأكيد, يسمح به `auto` ويسجله في `notes`. اختر `interaction: 'dialog'` للتأكيد قبل التثبيت. يفشل اختيار `silent` الصريح بالرمز `AUTHORIZER_REQUIRED` إذا غابت الصلاحيات أو لزم تأكيد النظام.
- تحفظ الإعدادات ترتيب طرق الصلاحيات وتفعيلها وخيارات التثبيت وتفضيلات إشعارات التقدم. يستخدم التثبيت من الرئيسية والتطبيقات الخارجية `dialog` افتراضيا, وتطبق خيارات `auto` أو `silent` المحفوظة صراحة. تحتفظ طلبات المضيف/البرامج النصية بخياراتها الصريحة وتبقى القيمة الافتراضية لواجهة البرامج النصية `auto`. لا تحفظ الاختيارات إلا بعد التأكيد.

******

### بداية سريعة

******

أمثلة `install` و`installAsync` و`session` و`uninstall` و`setDefault` لـ AutoJs6 >= 6.8.0 (5300). لا تعمل الدوال إلا عند استدعائها بالمصادر أو اسم الحزمة أو اختيار المثبت الافتراضي الذي حددته. استعلام الحالة الأول للقراءة فقط.

```js
// استعلام للقراءة فقط عن التوفر والتوافق.
console.log(installer.status);

// يتطلب تفويض Shizuku; يفشل silent إذا طلب Android التأكيد.
let installChosen = source => installer.install(source, {
    authorizer: 'shizuku', interaction: 'silent', deleteSource: false,
});

// تمثل المصفوفة حزما مستقلة ولكل عنصر نتيجة.
let installBatchChosen = sources => installer.installAsync(sources, {
    interaction: 'dialog', continueOnError: true, deleteSource: false,
}).then(results => results.forEach(result => console.log(result.ok, result.packageName, result.error)))
    .catch(error => console.error(error.code, error.systemMessage));

// تنتمي جميع الأجزاء إلى تطبيق واحد بما فيها APK الأساسي.
let installSplitsChosen = splitFiles => installChosen({ splits: splitFiles });

// تبدأ عند الاستدعاء; احتفظ بالجلسة لإلغائها أو انتظارها.
let watchChosen = source => {
    let session = installer.session(source, { interaction: 'dialog', deleteSource: false });
    session.on('stage', (stage, detail) => console.log(stage, detail))
        .on('progress', progress => console.log(Math.round(progress * 100) + '%'))
        .on('complete', result => console.log(result))
        .on('cancel', () => console.log('cancel'))
        .on('error', error => console.error(error.code, error.systemMessage));
    return session;
};

// مرر اسم الحزمة المقصودة فقط; يطلب keepData الاحتفاظ بالبيانات.
let uninstallChosen = packageName => installer.uninstall(packageName, {
    authorizer: 'shizuku', interaction: 'silent', keepData: true,
});

// true يعين الإضافة افتراضيا; false يمسح اختيارها. تطبق قيود ROM.
let setDefaultChosen = enabled => installer.setDefault(enabled, { authorizer: 'shizuku' });
```

يمكن أن يكون المصدر مسارا أو `file://` أو URI من نوع `content://` متاحا للقراءة. تمثل المصفوفة عناصر مستقلة, بينما يثبت `{ splits: [...] }` تطبيقا واحدا. تبدأ `session(...)` فور إنشائها, ويدعم الكائن المعاد `cancel()` و`wait()`. قد ترمي الاستدعاءات المتزامنة `InstallerError`, ولا يسمح بها على خيط UI. يشمل ذلك أيضا قراءة `installer.status` وإنشاء `installer.session(...)` واستدعاء `session.wait()`. استخدم طرق Async على خيط UI أو نفذ العمليات المتزامنة على خيط عمل للبرنامج النصي. يجب استخدام كائن الجلسة فقط على خيط البرنامج النصي الذي أنشأه. عالج رفض Promise وافحص `ok` و`error` لكل نتيجة في الدفعة. غياب الإضافة أو عدم توافقها يبلغ `PLUGIN_UNAVAILABLE`. تعيد `setDefault` ما إذا تحققت الحالة المطلوبة, بما فيها مسح الاختيار الافتراضي. تبقى `app.uninstall` اختصار المضيف لإزالة النظام; استخدم `installer.uninstall` للخيارات ذات الصلاحيات. راجع [توثيق installer API](https://docs.autojs6.com/#/installer) لجميع الخيارات والأحداث.

******

### التوافق

******

حقائق المنصة التي تحدد ما تستطيع الإضافة فعله:

- Android 7.0 (API 24) فأحدث. تسجل نتائج التحقق من الأجهزة والتغطية المتبقية في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- تجاوز حظر targetSdk المنخفض موجود اعتبارا من Android 14 (API 34). على الأنظمة الأقدم يتم تجاهل الخيار مع الإشارة إلى ذلك في النتيجة.
- قد تمنع سياسات ROM والإعدادات الافتراضية الحالية تغيير المثبت الافتراضي. لا يضمن الإصدار 1.0.0 قفل هذا الاختيار بشكل دائم. راجع الأسئلة الشائعة حول اسم حزمة المثبت وتفعيل الإضافة.

******

### الأسئلة الشائعة

******

- **لماذا ما زال التثبيت يطلب التأكيد?** يستخدم `none` تأكيد النظام دائما. جهز التفويض ثم اختر Shizuku أو Root في حوار التثبيت. قد يظل Android أو نهج الجهاز يطلب تأكيد النظام.
- **هل يمكن تثبيت ملف `.aab`?** لا. Android App Bundle صيغة نشر, فحوله أولا باستخدام bundletool إلى مجموعة `.apks`. تتعرف الإضافة على ملفات `.aab` وتعرض معلومات الحزمة والوحدات.
- **لماذا قد يفشل الرجوع لإصدار أقدم مع `allowDowngrade: true`?** يطلب هذا الخيار السماح بالرجوع فقط. يقرر Android بحسب البرنامج الثابت وهوية الصلاحيات وما إذا كان التطبيق debuggable. رفضت برامج user المختبرة على Sony G8441 / API 28 وXiaomi 23046RP50C / API 35 الرجوع لحزم غير debuggable, بينما قبله Sony XQ-DQ72 / API 33 مع Root. تخص هذه النتائج الأجهزة المذكورة فقط. راجع الخطأ و`systemMessage`; لا يضمن Root الرجوع على كل ROM.
- **ما اسم حزمة المثبت الذي يعمل على HyperOS?** عند تشغيل Shizuku عبر ADB أو التصحيح اللاسلكي, يستخدم `com.android.shell` إذا لم يحدد اسم حزمة المثبت. على جهاز Xiaomi 23046RP50C / HyperOS / API 35 الذي اختبر, سجل التثبيت الجديد والتحديث الصامتان هذه القيمة. قبل أيضا كل من `com.android.shell` واسم حزمة الإضافة عند تحديدهما صراحة, وسجلا كما طلبا. تظل الأسماء الأخرى أو إصدارات ROM خاضعة لرد النظام.
- **يطلب ColorOS أو نظام آخر تفعيل الإضافة. ماذا أفعل?** بعد التثبيت أو الإيقاف القسري, قد يبقي Android التطبيق متوقفا حتى يتفاعل معه المستخدم. استخدم إجراء التفعيل في مركز إضافات AutoJs6 إذا ظهر, أو افتح 3-Setup Installer من أيقونته ثم أعد المحاولة. يتوافق ذلك مع [قواعد حالة التوقف في Android](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_STOPPED). لم يتحقق بعد من سلوك ColorOS الخاص على جهاز فعلي.
- **لماذا قد يفشل تعيين المثبت الافتراضي?** قد يرفض نظام ROM التغيير. في إصدارات Android القديمة, قد يتطلب وجود معالج APK افتراضي مسح إعداداته الافتراضية أولا من إعدادات النظام; اتبع إرشادات الصفحة. إذا لم يوفر النظام إجراء المسح, فلا تضمن الإضافة الاستبدال. لا يضمن الإصدار 1.0.0 قفلا دائما حتى مع Shizuku أو Root.
- **لماذا لم يحذف المصدر?** تجري محاولة الحذف بعد نجاح التثبيت فقط. يحتفظ بالمصدر دائما عند فشل التثبيت أو إلغائه أو انتهاء مهلته. لا يغير فشل الحذف نجاح التثبيت, وقد يرفض المزود الخارجي الحذف. في السكربتات ينفذ المضيف `deleteSource` للمسارات ومصادر `file://` ويحتفظ بمصادر `content://`. راجع `sourceDeleted` و`notes`. في الدفعة, تظل العناصر المؤكد نجاحها خاضعة لـ `deleteSource` حتى إذا فشل عنصر آخر أو ألغي باقي الطابور.
- **هل يمكن إعادة المحاولة أو الاستئناف?** يمكن إعادة محاولة URI خارجي فاشل ما دام المصدر والوصول متاحين. بعد تحرير المصدر أو صلاحية الوصول, افتح الحزمة مجددا. بعد إعادة تشغيل العملية, تعرض الواجهة المستعادة النتائج المؤكدة المحفوظة وتحدد العناصر غير المكتملة على أنها منقطعة. الواجهة المستعادة للقراءة فقط ولا تثبت أو تعيد المحاولة تلقائيا. تحقق من حالة التطبيق المثبت قبل البدء مجددا.

******

### الصلاحيات والأمان

******

يلتزم المكون الإضافي بحدود صريحة:

- نقاط دخول Binder محمية بإذن التوقيع `org.autojs.permission.PLUGIN`, لذا لا يصل إليها سوى AutoJs6. مدخل "فتح باستخدام" الخارجي لا يقبل إلا ملفات الحزم ولا يشغل أي برنامج نصي أبدا.
- تدعم REQUEST_INSTALL_PACKAGES وREQUEST_DELETE_PACKAGES تأكيد Android. تستخدم QUERY_ALL_PACKAGES لإدارة التطبيقات المثبتة ومقارنة الإصدارات والتوقيعات واكتشاف المثبت الافتراضي.
- يدعم FOREGROUND_SERVICE وFOREGROUND_SERVICE_DATA_SYNC عمل التثبيت في الخلفية, ويتيح POST_NOTIFICATIONS إشعارات التقدم والنتائج. غياب إذن الإشعارات لا يمنع التثبيت.
- لا يستخدم Shizuku و Root إلا للعملية التي تبدأها أنت. الخدمة ذات الامتيازات لا تحتفظ بحالة ولا تبقي أي shell مفتوحا بين العمليات ولا يمكن الوصول إليها من خارج الإضافة.
- يعمل التثبيت والفحص والسجل وإدارة التطبيقات دون اتصال. تستخدم INTERNET فقط عند فحص الإصدارات يدويا عبر واجهة GitHub Releases الثابتة للملحق بفاصل 12 ساعة. لا تجرى فحوص تحديث في الخلفية ولا ترفع الحزم.
- تفتح مصادر الحزم للقراءة فقط. يخزن السجل بيانات وصفية محدودة ونتائج دون محتويات الحزم أو URI المصادر, وتخفى المسارات في الأخطاء. يستثنى التخزين الخاص من النسخ الاحتياطية. لا يؤدي حذف سجل إلى إزالة التطبيق أو حذف مصدره.

بعد النشر الرسمي, احصل على المكون الإضافي فقط من صفحة [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/releases) الرسمية أو من مركز المكونات الإضافية في AutoJs6. قد تفشل الحزم من مصادر غير معروفة في التحقق من المضيف أو تحمل مخاطر حتى لو بدا رقم الإصدار متطابقا.

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

تستجيب `ThreeSetupInstallerPluginService` للإجراء `org.autojs.plugin.INSTALLER` (category `installer`) وتنفذ اتفاقية installer-api الخاصة بالمضيف `org.autojs.plugin.installer.api.IInstallerPlugin`. تستجيب `ThreeSetupInstallerPluginInfoService` للإجراء `org.autojs.plugin.INFO` بكائن PluginInfo. يتيح `WakeActivity` للمضيف تنشيط الإضافة.

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

- `تلميح` يوثق 1.0.0 ميزات التثبيت وإدارة التطبيقات والبرامج النصية المنفذة أدناه. لم يكتمل بعد النشر الرسمي في GitHub Releases والإدراج في مركز الإضافات. يتطلب التكامل AutoJs6 >= 6.8.0 (5299), وتتطلب واجهة البرامج النصية `installer` البناء 5300 أو أحدث. تسجل تغطية الأجهزة والتحققات المتبقية في [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/ROADMAP.md).
- `ميزة` يثبت 3-Setup Installer تطبيقات Android ويحدثها ويفحصها ويزيلها من شاشته الرئيسية المستقلة, أو عبر مداخل AutoJs6 وبرامجه النصية, أو طلبات فتح الحزم ومشاركتها من التطبيقات الخارجية. يدعم تأكيد Android والعمليات ذات الصلاحيات عبر Shizuku أو Root
- `ميزة` صيغ الحزم: `.apk` و `.apks` و `.xapk` و `.apkm` و `.apkz` وأرشيفات ZIP التي تحتوي على ملفات APK. تختار الإضافة الحزم المقسمة المناسبة للجهاز, وتتعرف على ملفات `.aab` وتصفها دون تثبيتها
- `ميزة` يمكن طلب محاولة حذف المصدر بعد نجاح التثبيت. تتطلب العودة لإصدار أقدم وحزم الاختبار وتجاوز حد targetSdk المنخفض (Android 14+) وتحديد المثبت والمستخدمين الآخرين Shizuku أو Root, وتظل خاضعة لقيود Android
- `ميزة` توفر واجهة السكربت `installer` (الاسم البديل `$installer`) صيغ التزامن و`...Async` والجلسات للتثبيت الفردي والدفعات والحزم المقسمة, والإزالة والفحص والاستعلام عن طرق التفويض والمستخدمين وتعيين المثبت الافتراضي. الأخطاء من نوع `InstallerError` بقيم `code` ثابتة (يتطلب AutoJs6 >= 6.8.0 (5300)). تستخدم البرامج النصية `interaction: 'auto'` افتراضيا وتثبت بصمت عند توفر الصلاحيات. تستخدم إجراءات التثبيت في واجهة المضيف `dialog`. إذا طلب Android التأكيد, يسمح به `auto` ويسجله في `notes`. اختر `interaction: 'dialog'` للتأكيد قبل التثبيت. يفشل اختيار `silent` الصريح بالرمز `AUTHORIZER_REQUIRED` إذا غابت الصلاحيات أو لزم تأكيد النظام
- `ميزة` تعرض الشاشة الرئيسية المستقلة توفر Shizuku/Root وصلاحياتهما والمثبت الافتراضي والمهام النشطة وعمليات التثبيت الأخيرة. اختر عدة حزم من منتقي ملفات النظام لتثبيتها بالتتابع, أو المتابعة بعد فشل عنصر, أو إلغاء العناصر المتبقية
- `ميزة` يعرض التأكيد معلومات التطبيق والإصدارين القديم والجديد والتواقيع ومكونات APK القابلة للاختيار. يمكن إلغاء العملية أثناء تقدمها, وتعرض النتائج إجراءات النجاح أو تفاصيل الخطأ القابلة للنسخ. يعرض التثبيت الجماعي حالة كل عنصر
- `ميزة` إشعارات تقدم التثبيت في الخدمة الأمامية والإلغاء والنتائج. رفض إذن الإشعارات لا يمنع التثبيت
- `ميزة` افتح أو شارك حزمة واحدة أو عدة حزم, بما فيها ملفات APKS التي يشاركها MT Manager. تدخل الحزم المتعددة في طابور متتابع. يمكن إعادة محاولة العناصر الخارجية الفاشلة ما دام URI وإذن الوصول متاحين
- `ميزة` تتيح إدارة التطبيقات المثبتة البحث بالاسم أو اسم الحزمة والترتيب حسب الاسم أو وقت التثبيت أو التحديث وإظهار تطبيقات النظام. يمكنك فتح التطبيق أو معلوماته في النظام, أو مراجعة إزالته وتأكيدها. يمكن لـ Shizuku أو Root الإزالة بعد التأكيد دون طلب نظام إضافي مع الاحتفاظ بالبيانات اختياريا, وتستخدم الحالات الأخرى تأكيد Android
- `ميزة` تفتح بطاقة الحالة الرئيسية والإعدادات صفحة المثبت الافتراضي نفسها, مع تعيينه أو إلغائه بصلاحيات وإرشادات إلى إعدادات النظام عند غياب الصلاحيات. قد تمنع سياسات الشركة المصنعة التغيير أو تتطلب مسح المعالج السابق. تبقى `installer.isDefault` و`installer.setDefault` و`setDefaultAsync` متاحة للبرامج النصية, وتعكس النتائج استجابة الجهاز
- `ميزة` تحفظ الإعدادات ترتيب طرق الصلاحيات وتفعيلها وخيارات التثبيت وتفضيلات إشعارات التقدم. يستخدم التثبيت من الرئيسية والتطبيقات الخارجية `dialog` افتراضيا, وتطبق خيارات `auto` أو `silent` المحفوظة صراحة. تحتفظ طلبات المضيف/البرامج النصية بخياراتها الصريحة وتبقى القيمة الافتراضية لواجهة البرامج النصية `auto`. لا تحفظ الاختيارات إلا بعد التأكيد
- `ميزة` تشمل إعدادات المظهر اللغة والوضع الليلي ولون السمة وأيقونة المشغل. تتبع العناصر الثلاثة الأولى AutoJs6 افتراضيا وتقبل تجاوزات محلية; وعند غياب المضيف تستخدم لغة النظام ووضعه واللون الافتراضي. تتوفر للأيقونة أوضاع فاتح وداكن وتلقائي وشفاف; يتبع التلقائي النظام ويتأثر بالتخزين المؤقت وأقنعة المشغل
- `ميزة` يحتفظ سجل التثبيت الخاص بما يصل إلى 200 عنصر تشمل الحزمة والاسم والإصدار القديم/الجديد والنتيجة والوقت والمصدر (المضيف/برنامج نصي/خارجي/الرئيسية) وطريقة الصلاحيات وتفاصيل الفشل. يمكنك حذف سجل فردي أو مسح السجل دون إزالة التطبيقات أو حذف الملفات المصدرية. بعد انتهاء العملية تصبح العناصر غير المكتملة ملغاة ولا تستأنف تلقائيا
- `ميزة` تتوفر صفحة حول وسجل الإصدارات المدمج بعشر لغات من الإعدادات. يستخدم فحص التحديث اليدوي واجهة GitHub Releases الخاصة بالملحق بفاصل 12 ساعة مع نتائج مخزنة مؤقتا وإدارة الإصدارات المتجاهلة. تفتح صفحات الإصدارات في المتصفح, ولا تنزل التحديثات أو تثبت تلقائيا
- `ميزة` README وتعليمات مركز الإضافات وسجل التغييرات بعشر لغات
- `تحسين` تجنب المصادر ذات الوصول العشوائي نسخ الملف بالكامل إلى الذاكرة المؤقتة, مع تخزين التدفقات مؤقتا عند الحاجة. تدعم حزم ZIP المجزأة, وتقتصر ملفات AAB على الفحص, وترفض المصادر التي تغير محتواها
- `تحسين` تجري محاولة الحذف بعد نجاح التثبيت فقط. يحتفظ بالمصدر دائما عند فشل التثبيت أو إلغائه أو انتهاء مهلته. لا يغير فشل الحذف نجاح التثبيت, وقد يرفض المزود الخارجي الحذف. في السكربتات ينفذ المضيف `deleteSource` للمسارات ومصادر `file://` ويحتفظ بمصادر `content://`. راجع `sourceDeleted` و`notes`. في الدفعة, تظل العناصر المؤكد نجاحها خاضعة لـ `deleteSource` حتى إذا فشل عنصر آخر أو ألغي باقي الطابور
- `تحسين` تنفيذ عمليات تثبيت الحزمة نفسها بالتسلسل عبر المستخدمين وطرق التفويض مع دعم الإلغاء والمهلة أثناء الانتظار, وتنظيف مجلدات التجهيز غير النشطة لأكثر من 24 ساعة عند التشغيل المستقل
- `تحسين` إعادة محاولة الاتصال ذي الصلاحيات مرة واحدة إذا انقطع أثناء إنشائه; لا تعاد تلقائيا عمليات التثبيت أو الإزالة التي بدأت بالفعل
- `تبعية` إضافة Shizuku API 13.1.5 (`dev.rikka.shizuku:api` و `dev.rikka.shizuku:provider`) لطريقة تفويض Shizuku
- `تبعية` إضافة libsu 6.0.0 (`com.github.topjohnwu.libsu:core` و `service`) لطريقة تفويض Root
- `تبعية` إضافة AndroidHiddenApiBypass 6.1 لواجهات مثبت الحزم المخفية التي تستخدمها الخدمة ذات الامتيازات
- `تبعية` إضافة `common-plugin-api.aar` (وحدة AutoJs6 `plugin-api/common-plugin-api`, بنية المضيف 6.8.0 / 5298, MPL 2.0) كاتفاقية إضافات مشتركة مع قفل التجزئة في `locks/host-api-aars.lock`
- `تبعية` إضافة `installer-api.aar` (AutoJs6, MPL 2.0) لعقد التثبيت; المصدر وSHA-256 مسجلان في إشعارات الجهات الخارجية
- `تبعية` إضافة `package-archive-parser.aar` (AutoJs6, MPL 2.0) لفحص APK والحاويات واختيار الأجزاء; المصدر وSHA-256 مسجلان في إشعارات الجهات الخارجية

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Setup-Installer/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء والتحقق

******

يمكن للمطورين بناء الإضافة والتحقق منها بالأوامر التالية. قبل النشر الرسمي, استخدم بناء من المشرف أو بناء محليا للاختبار. ستوزع ملفات APK المنشورة عبر Releases ومركز الإضافات بعد الفهرسة.

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
