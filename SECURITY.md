# Firebase güvenlik yapılandırması

Yetkilerin kaynağı kökteki `firestore.rules` ve `storage.rules` dosyalarıdır. Tüm veritabanını açan geliştirme kuralları kullanılmaz.

- Kullanıcı profillerini yalnızca ilgili kullanıcı okuyabilir ve değiştirebilir.
- Restoran içeriğini yalnızca kayıtlı sahibi değiştirebilir. Sahiplik ve puanlar istemciden değiştirilemez.
- Yorumlar `restaurants/{restaurantId}/reviews/{reviewId}` altında tutulur. Müşteri kendi yorumunu ekler/siler; restoran sahibi yalnızca `ownerReply` alanını değiştirir.
- Müşteri istekleri kimliği doğrulanmış kullanıcıya bağlanır. İsteği yalnızca gönderen ve ilgili restoran sahibi okuyabilir; tamamlamayı sahibi yapar.
- Görseller `restaurant_images/{uid}/{fileName}` altında, en fazla 5 MB JPEG/PNG/WebP olarak yüklenir. Başka kullanıcı bu dosyaları değiştiremez.
- Yeni uygulama sürümü yorumları okuyarak puanları hesaplar; istemci ortak puan alanına yazmaz. Bu hesaplama sunucuda doğrulanmış satın alma/ziyaret kanıtı değildir.

## Doğrulama

```sh
cd security-tests
npm ci --ignore-scripts
cd ..
firebase emulators:exec --only firestore,storage --project demo-restaview-security "node --test security-tests/rules.test.cjs"
```

Test bağımlılığındaki gRPC override, upstream sürüm aralığının seçtiği güvenlik açığı bulunan sürümü düzeltir. Testler bu override ile çalıştırılmıştır.

## Yayına geçiş

Her iki uygulama aynı Firebase projesini kullanır. Kuralları tek bir depodan dağıtın:

```sh
firebase deploy --only firestore:rules,storage --project restoranapp-c16f7
```

Eski uygulama sürümlerinin tüm restoranı güncelleyen yorum işlemleri yeni kurallarla reddedilir; iki Android uygulamasını birlikte güncelleyin.

14 Ekim 2026 geçişinde 22 restoran kontrol edildi ve 20 eski yorum ayrı belgelere kopyalandı. Her restoran için atomik yazma ve sürüm önkoşulu kullanıldı. Orijinal yorum dizileri korundu, `reviewsMigrated` işareti eklendi. Yeni istemci bu işaret varsa eski diziyi okumaz; orijinal dizi istemciler için değiştirilemez. Yedek ve geçiş kayıtları yayımlanmaz.

Eski kayıtlar önceden herkese açık olduğundan, geçmişte değiştirilmedikleri garanti edilemez. Restoran sahipliğini ve eski yorumları hesap sahibi ayrıca doğrulamalıdır. Sahipsiz/demo restoranlara otomatik sahip ataması yapılmaz.
