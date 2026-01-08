**NetraGuide – Aplikasi “Digital Eye” Berbasis AI untuk Tunanetra**
NetraGuide adalah aplikasi Android berbasis Artificial Intelligence yang dirancang untuk membantu penyandang tunanetra mengenali lingkungan sekitarnya secara mandiri. 
Aplikasi ini bekerja sebagai digital eye dengan memanfaatkan kamera smartphone untuk mendeteksi objek di sekitar pengguna dan mengubah informasi visual tersebut menjadi feedback audio (Text-to-Speech) secara real-time.

**Tujuan utama NetraGuide** 
Meningkatkan kemandirian, mobilitas, dan keamanan pengguna tunanetra dalam menjalani aktivitas sehari-hari, sekaligus mengatasi keterbatasan alat bantu konvensional (tongkat putih) yang tidak mampu mendeteksi objek melayang atau memberikan informasi jenis objek.

**Teknologi & Library yang Digunakan**
NetraGuide dibangun menggunakan teknologi mobile dan machine learning yang dioptimalkan untuk perangkat Android:
1. Kotlin & Jetpack Compose
   Digunakan sebagai bahasa dan framework utama untuk pengembangan aplikasi Android dan UI pengguna.
2. TensorFlow Lite (TFLite)
   Berfungsi sebagai mesin inferensi machine learning agar model AI dapat berjalan langsung di smartphone dengan latensi rendah.
3. YOLOv8 Nano (YOLOv8n)
   Model object detection yang ringan dan cepat, dipilih karena memiliki keseimbangan terbaik antara kecepatan dan akurasi untuk perangkat mobile.
4. Text-to-Speech (TTS)
   Digunakan untuk mengonversi hasil deteksi objek menjadi suara agar dapat langsung didengar oleh pengguna.
5. GPU Delegate (Android)
   Dimanfaatkan untuk mempercepat proses inferensi AI secara real-time.

**Platform & Lingkungan Pengembangan**
Aplikasi NetraGuide dikembangkan dan diuji pada platform Android.
Proses development dilakukan menggunakan Android Studio dengan fokus pada performa real-time, efisiensi daya, dan kemudahan penggunaan bagi tunanetra.

**Data & Model AI**
- Dataset: COCO Dataset (Common Objects in Context)
- Skala Data: Lebih dari 328.000 gambar dengan 80 kategori objek umum (manusia, kursi, botol, dll).
- Alasan Pemilihan: Dataset standar industri dengan variasi tinggi untuk menjamin akurasi dan keandalan sistem.

**Cara Kerja Aplikasi**
1. Kamera Smartphone menangkap video lingkungan sekitar pengguna.
2. Preprocessing: Gambar diputar sesuai orientasi HP, di-resize ke ukuran 640×640, dan dinormalisasi.
3. Inference AI: Model YOLOv8n (TFLite) mendeteksi objek dan memprediksi jenis serta tingkat kepercayaannya.
4. Post-Processing: Hasil deteksi difilter menggunakan confidence threshold dan Non-Maximum Suppression (NMS).
5. Output: Nama objek dan tingkat akurasi ditampilkan di layar serta dibacakan melalui audio (TTS).

**Fitur Utama**
1. Deteksi Objek Real-Time: Mendeteksi objek fisik di sekitar pengguna tanpa perlu kontak langsung.
2. Feedback Audio (Text-to-Speech): Informasi objek langsung dibacakan agar mudah dipahami oleh tunanetra.
3. Privasi Terjamin: Seluruh proses AI berjalan di HP, tidak ada data video yang dikirim ke server eksternal.
4. Offline Mode: Aplikasi tetap berfungsi tanpa koneksi internet.

