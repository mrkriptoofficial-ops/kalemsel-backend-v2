package com.gezgin.zikirmatik

import android.content.SharedPreferences

// Otomatik üretildi: tools/gen_encourage.py — elle değiştirmeyin.
/** Hedef tamamlanınca gösterilen (metin, kaynak) havuzu. */
val ENCOURAGE_ALL: List<Pair<String, String>> = listOf(
    "Siz beni anın ki ben de sizi anayım." to "Bakara 152",
    "Kullarım sana beni sorarlarsa, bilsin ki ben yakınım; dua eden kimsenin duasına icabet ederim." to "Bakara 186",
    "Rabbimiz! Bize dünyada iyilik ver, ahirette de iyilik ver ve bizi ateş azabından koru." to "Bakara 201",
    "Allah hiçbir nefse gücünün yetmeyeceğinden fazlasını yüklemez." to "Bakara 286",
    "Ey iman edenler! Sabır ve namazla yardım dileyin. Şüphesiz Allah sabredenlerle beraberdir." to "Bakara 153",
    "Allah'ın rahmetinden ümit kesmeyin, çünkü Allah bütün günahları bağışlar." to "Zümer 53",
    "Şüphesiz Allah çok tövbe edenleri ve temizlenenleri sever." to "Bakara 222",
    "Kendiniz için önceden yaptığınız hayrı Allah katında bulursunuz." to "Bakara 110",
    "Biz Allah'a aidiz ve dönüşümüz de O'nadır." to "Bakara 156",
    "Rabbimiz! Bizi doğru yola ilettikten sonra kalplerimizi eğriltme ve bize katından bir rahmet bağışla." to "Âl-i İmrân 8",
    "Üzülmeyin, hüzünlenmeyin; eğer mümin iseniz, üstün olan sizsiniz." to "Âl-i İmrân 139",
    "Kararını verdin mi artık Allah'a tevekkül et." to "Âl-i İmrân 159",
    "Bize Allah yeter; O ne güzel vekildir." to "Âl-i İmrân 173",
    "Namazı bitirince ayakta, otururken ve yan yatarken Allah'ı anın." to "Nisâ 103",
    "Allah'a korkuyla ve umutla dua edin; Allah'ın rahmeti iyilik yapanlara yakındır." to "A'râf 56",
    "Rabbimiz! Kendimize zulmettik; bizi bağışlamaz ve bize merhamet etmezsen mutlaka ziyana uğrayanlardan oluruz." to "A'râf 23",
    "Rabbini içinden, yalvararak ve korkarak, sabah akşam an; gafillerden olma." to "A'râf 205",
    "En güzel isimler Allah'ındır; onlarla O'na dua edin." to "A'râf 180",
    "Allah anıldığında kalbi titreyen, ayetleri okunduğunda imanı artan kimselerdir müminler." to "Enfâl 2",
    "Dikkat edin, kalpler ancak Allah'ı anmakla huzur bulur." to "Ra'd 28",
    "Şükrederseniz, elbette nimetimi artırırım." to "İbrâhîm 7",
    "Kitaptan sana vahyedileni oku ve namazı kıl; çünkü Allah'ı anmak elbette en büyük ibadettir." to "Ankebût 45",
    "Ey iman edenler! Allah'ı çok zikredin ve sabah akşam O'nu tesbih edin." to "Ahzâb 41-42",
    "Allah'ı çok zikreden erkeklere ve kadınlara Allah bağışlanma ve büyük mükâfat hazırlamıştır." to "Ahzâb 35",
    "Andolsun, Allah'ın Resûlünde sizin için güzel bir örnek vardır." to "Ahzâb 21",
    "Şüphesiz Allah ve melekleri Peygamber'e salat eder. Ey iman edenler, siz de ona salat edin." to "Ahzâb 56",
    "Beni anmak için namaz kıl." to "Tâhâ 14",
    "Korkmayın, şüphesiz ben sizinle beraberim; işitir ve görürüm." to "Tâhâ 46",
    "Zikri biz indirdik ve onu koruyacak olan da biziz." to "Hicr 9",
    "Gerçekten zorlukla beraber bir kolaylık vardır." to "İnşirâh 5-6",
    "Kim Allah'a karşı gelmekten sakınırsa, Allah ona bir çıkış yolu yaratır." to "Talâk 2",
    "Kim Allah'a tevekkül ederse, O ona yeter." to "Talâk 3",
    "Kim mümin olarak iyi bir iş yaparsa, ona güzel bir hayat yaşatırız." to "Nahl 97",
    "Şüphesiz Allah, takva sahipleri ve iyilik yapanlarla beraberdir." to "Nahl 128",
    "Mal ve oğullar dünya hayatının süsüdür; kalıcı olan iyi ameller Rabbin katında daha hayırlıdır." to "Kehf 46",
    "Nerede olursanız olun O sizinle beraberdir." to "Hadîd 4",
    "Üzülme, şüphesiz Allah bizimle beraberdir." to "Tevbe 40",
    "Dikkat edin, Allah'ın dostlarına korku yoktur ve onlar üzülmezler." to "Yûnus 62",
    "Ey Rabbimiz! Bize, gözümüzü aydınlatacak eşler ve soylar bağışla." to "Furkân 74",
    "Hiç şüphesiz Allah, iyilik edenleri sever." to "Bakara 195",
    "Rabbiniz şöyle buyurdu: Bana dua edin, size icabet edeyim." to "Mü'min 60",
    "Allah'ın nimetlerini sayarsanız bitiremezsiniz." to "İbrâhîm 34",
    "Sabredenlere mükâfatları hesapsız verilecektir." to "Zümer 10",
    "Yalnız Sana kulluk ederiz ve yalnız Senden yardım dileriz." to "Fâtiha 5",
    "Hamd, âlemlerin Rabbi olan Allah'a mahsustur." to "Fâtiha 2",
    "Rabbimiz! Bizi unutur veya hata edersek bizi sorumlu tutma." to "Bakara 286",
    "Rabbimiz! Bize sabır ver, ayaklarımızı sabit kıl ve kâfir topluluğa karşı yardım et." to "Bakara 250",
    "Allah'a sığınırım, ne olursa olsun O bize yeter." to "Âl-i İmrân 173",
    "İnsanın yalnız çalıştığı şeyin karşılığı vardır." to "Necm 39",
    "Dua ibadetin özüdür." to "Tirmizî, Ebû Dâvûd",
    "Kur'an'dan bir harf okuyan kimseye bir sevap vardır; her sevap da on katıyla verilir." to "Tirmizî",
    "Sizin en hayırlınız, Kur'an'ı öğrenen ve öğretendir." to "Buhârî",
    "Kolaylaştırın, zorlaştırmayın; müjdeleyin, nefret ettirmeyin." to "Buhârî, Müslim",
    "Kim bir müminin dünya sıkıntılarından birini giderirse, Allah da kıyamet günü onun sıkıntılarından birini giderir." to "Müslim",
    "Kim ilim öğrenmek için bir yola girerse, Allah ona cennete giden yolu kolaylaştırır." to "Müslim",
    "Hiçbiriniz kendisi için istediğini kardeşi için de istemedikçe iman etmiş olmaz." to "Buhârî, Müslim",
    "Sizin en hayırlınız, ahlakı en güzel olanınızdır." to "Buhârî, Müslim",
    "Güçlü, güreşte yenen değil; öfke anında nefsine hâkim olandır." to "Buhârî, Müslim",
    "Sadaka mal eksiltmez." to "Müslim",
    "Mümin bir delikten iki defa ısırılmaz." to "Buhârî, Müslim",
    "Kim bir hayra vesile olursa, onu yapanın sevabı gibi sevabı vardır." to "Müslim",
    "Kim bir gün içinde yüz defa lâ ilâhe illallâhu vahdehû lâ şerîke leh derse, on köle azat etmiş gibi olur." to "Buhârî, Müslim",
    "İki nimet vardır ki insanların çoğu bunlarda aldanır: sağlık ve boş vakit." to "Buhârî",
    "Sabır aydınlıktır." to "Müslim",
    "Ameller niyetlere göredir." to "Buhârî, Müslim",
    "Allah sizin suretlerinize ve mallarınıza değil, kalplerinize ve amellerinize bakar." to "Müslim",
    "Allah'ın en sevdiği amel, az da olsa devamlı olanıdır." to "Buhârî, Müslim",
    "Allah'ın en sevdiği dört söz: Sübhânallah, elhamdülillah, lâ ilâhe illallah ve Allahu ekber." to "Müslim",
    "Temizlik imanın yarısıdır; elhamdülillah mizanı doldurur." to "Müslim",
    "Sübhânallâhi ve bihamdihî, Sübhânallâhi'l-azîm; bu iki kelime dile hafiftir, mizanda ağırdır, Rahmân'a sevimlidir." to "Buhârî, Müslim",
    "Bana bir kez salavat getiren kimseye Allah on rahmet eder." to "Müslim",
    "Mizanda güzel ahlaktan daha ağır basan bir şey yoktur." to "Tirmizî, Ebû Dâvûd",
    "Her kim Allah'a ve ahiret gününe inanıyorsa, ya hayır söylesin ya da sussun." to "Buhârî, Müslim",
    "En faziletli amel, Allah'a ve Resûlü'ne iman etmektir." to "Buhârî, Müslim",
    "Bir kişinin Allah yolunda bir gün nöbet tutması, dünya ve içindekilerden hayırlıdır." to "Buhârî, Müslim",
    "Dünya, mü'minin zindanı ve kâfirin cennetidir." to "Müslim",
    "Mü'minin durumuna hayret ederim; her işi hayırlıdır." to "Müslim",
    "Kim gece uyanıp 'lâ ilâhe illallâh' der ve dua ederse, duası kabul olunur." to "Buhârî",
    "Allah'ım! Senden affını ve sağlığı isterim." to "Tirmizî, İbn Mâce",
    "Kişi, Allah'ın rahmetiyle kurtulur; amel tek başına yetmez." to "Buhârî, Müslim",
    "Allah'ı zikredenle zikretmeyen, diri ile ölü gibidir." to "Buhârî, Müslim",
    "Ey insanlar, selamı yayın, yemek yedirin, akrabanızı ziyaret edin ve insanlar uyurken namaz kılın; cennete selametle girersiniz." to "Tirmizî, İbn Mâce",
    "Kim Allah'a ve ahiret gününe inanıyorsa komşusuna ikramda bulunsun." to "Buhârî, Müslim",
    "Dünyada bir garip veya yolcu gibi ol." to "Buhârî",
    "Allah Teâlâ buyurur: Ben kulumun beni zannettiği gibiyim; beni andığı zaman onunla beraberim." to "Buhârî, Müslim",
    "Ey Muâz! Her namazın ardından: Allah'ım, seni anmama, sana şükretmeme ve sana güzel ibadet etmeme yardım et, demeyi ihmal etme." to "Ebû Dâvûd, Nesâî",
    "Allah'ım! Sen selamsın, selam sendendir; ey celâl ve ikram sahibi, bereketlisin." to "Müslim",
    "Kim akşamleyin üç kez A'ûzü bi kelimâtillâhi't-tâmmâti min şerri mâ halak derse, o geceki zararlardan korunur." to "Müslim",
    "Asıl zenginlik, mal çokluğu değil, gönül zenginliğidir." to "Buhârî, Müslim",
    "Kim bir müslümanın ayıbını örterse, Allah da dünyada ve ahirette onun ayıbını örter." to "Müslim",
    "Müslüman, elinden ve dilinden müslümanların emin olduğu kimsedir." to "Buhârî, Müslim",
    "Merhamet etmeyene merhamet olunmaz." to "Buhârî, Müslim",
    "Ey iman edenler! Bir topluluğa karşı karşılaştığınızda sebat edin ve Allah'ı çok zikredin ki kurtuluşa eresiniz." to "Enfâl 45",
    "Namaz bittiğinde yeryüzüne dağılın, Allah'ın lütfundan arayın ve Allah'ı çokça zikredin ki kurtuluşa eresiniz." to "Cuma 10",
    "Rablerine sabah akşam, O'nun rızasını isteyerek dua edenlerle beraber nefsini alıkoy." to "Kehf 28",
    "İnsanlara teşekkür etmeyen, Allah'a da şükretmiş olmaz." to "Ebû Dâvûd, Tirmizî",
    "Rabbim! Göğsümü genişlet ve işimi kolaylaştır." to "Tâhâ 25-26",
    "Rabbimiz! Günahlarımızı ve işimizdeki aşırılıklarımızı bağışla, ayaklarımızı sabit kıl ve kâfirler topluluğuna karşı bize yardım et." to "Âl-i İmrân 147",
    "Benim başarım yalnız Allah'ın yardımıyladır; O'na tevekkül ettim ve O'na yönelirim." to "Hûd 88",
    "Allah'ım! Senden hidayet, takva, iffet ve gönül zenginliği isterim." to "Müslim"
)

/**
 * Havuzu karıştırıp tüketir; tüm havuz bitmeden aynı kayıt tekrar gelmez.
 * Sıra ve kalan kayıtlar SharedPreferences'ta tutulur: uygulama yeniden açılsa da devam eder.
 * Havuz bitince yeniden karıştırır; önceki kayıt başa gelmez.
 */
class EncouragePicker(
    private val prefs: SharedPreferences,
    private val pool: List<Pair<String, String>> = ENCOURAGE_ALL,
) {
    private val kDeck = "enc_deck"
    private val kLast = "enc_last"

    private fun load(): MutableList<Int> =
        (prefs.getString(kDeck, "") ?: "").split(',').mapNotNull { it.toIntOrNull() }
            .filter { it in pool.indices }.toMutableList()

    private fun save(deck: List<Int>, last: Int) {
        prefs.edit().putString(kDeck, deck.joinToString(",")).putInt(kLast, last).apply()
    }

    fun next(): Pair<String, String> {
        if (pool.isEmpty()) return "" to ""
        val deck = load()
        val last = prefs.getInt(kLast, -1)
        if (deck.isEmpty()) {
            deck.addAll(pool.indices.shuffled())
            if (deck.size > 1 && deck.first() == last) deck.add(deck.removeAt(0))
        }
        val idx = deck.removeAt(0)
        save(deck, idx)
        return pool[idx]
    }
}
