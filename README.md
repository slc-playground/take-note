## Bilinen Eksikler

- Yeni bir yorum eklediğimde (Örn: 10. satır) dosyayı kapatıp tekrar açtığımda yorum ikonu bir alt satırda görünüyor (Örn: 11. satır)

- Bir dosyada ilk defa bir satıra yorum eklendikten sonra dosyayı kapatıp açtığımda `notes.json` dosyasına bu kayıt yansımıyor.
Editörde canlı değişiklik yapmaya devam ettiğimde yorum ikonu yanlış konumlanıyor.

- Idea başlatıldığında editörde açık olan dosyalar varsa (kapanmadan önce açık kalan dosyalar) bu dosyalara ait yorumlar yüklenmiyor.
Dosyayı kapatıp açtığımda geri geliyor.

- `notes.json`da kaydı bulunan bir dosyadaki yorum satırından öncesine yeni satırlar eklediğimde editörde canlı olarak yorum ikonu konumlanması doğru çalışıyor.
Fakat dosyayı kapatıp açtığımda yorum ikonunun eski konumunda kaldığını görüyorum. Güncel lineNumber değeri `notes.json` dosyasına yansımamış oluyor.


## Yapılacaklar

- Yorumun tarih değerinin görüntülenmesinde yıl bilgisine de yer vererek şöyle bir düzenleme yapılabilir.
Bulunduğumuz takvim yılına ait bir yorum ise yıl bilgisi gösterilmesin ve gün değeri görünsün. Geçmiş yıllara ait ise yıl değeri yazarken gün bilgisi yazmasın.
  
  Örn: 6 Haz Cum, 00:19 veya 24 Kas 2025, 20:45 gibi

- Popup penceresi tasarımı iyileştirilebilir.

- Eklenti adı "LineNote" olarak değiştirilebilir. Uygun bir ikon tasarımı da başka bir AI ile ürettirilebilir.
Ayrıca uygulamanın paket yapısı da eklenti adına uyumlu şekilde güncellenecek!