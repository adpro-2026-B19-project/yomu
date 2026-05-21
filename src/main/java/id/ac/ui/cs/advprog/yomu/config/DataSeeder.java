package id.ac.ui.cs.advprog.yomu.config;

import id.ac.ui.cs.advprog.yomu.achievement.model.DailyMission;
import id.ac.ui.cs.advprog.yomu.achievement.repository.DailyMissionRepository;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthRole;
import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.repository.AuthRepository;
import id.ac.ui.cs.advprog.yomu.league.model.Clan;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMember;
import id.ac.ui.cs.advprog.yomu.league.model.ClanMemberRole;
import id.ac.ui.cs.advprog.yomu.league.model.Tier;
import id.ac.ui.cs.advprog.yomu.league.model.TierCode;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanMemberRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.ClanRepository;
import id.ac.ui.cs.advprog.yomu.league.repository.TierRepository;
import id.ac.ui.cs.advprog.yomu.reading.model.Category;
import id.ac.ui.cs.advprog.yomu.reading.model.Option;
import id.ac.ui.cs.advprog.yomu.reading.model.Question;
import id.ac.ui.cs.advprog.yomu.reading.model.Text;
import id.ac.ui.cs.advprog.yomu.reading.repository.CategoryRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.OptionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.QuestionRepository;
import id.ac.ui.cs.advprog.yomu.reading.repository.TextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TextRepository textRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final AuthRepository authRepository;
    private final DailyMissionRepository dailyMissionRepository;
    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final TierRepository tierRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedReadingModule();
        seedOptInDemoData();
    }

    private void seedAdminUser() {
        if (isDockerProfileActive() && !isDemoSeedEnabled()) {
            return;
        }

        String adminUsername = "cat";
        String adminEmail = "hasanul.muttaqin@ui.ac.id";
        String adminDisplayName = "Hasanul Muttaqin";
        String encodedPassword = passwordEncoder.encode("pass123");

        List<AuthUser> conflictingUsers = authRepository
                .findAllByEmailIgnoreCaseOrUsernameIgnoreCase(adminEmail, adminUsername);
        if (!conflictingUsers.isEmpty()) {
            authRepository.deleteAll(conflictingUsers);
        }

        AuthUser admin = new AuthUser(
                adminUsername,
                adminEmail,
                null,
                adminDisplayName,
                encodedPassword,
                AuthRole.ADMIN
        );
        authRepository.save(admin);
    }

    private void seedOptInDemoData() {
        if (!isDemoSeedEnabled()) {
            return;
        }

        AuthUser leader = ensureUser(
                "kalfin-demo-leader",
                "kalfin.demo.leader@yomu.test",
                "Kalfin Demo Leader",
                "KalfinDemo1!",
                AuthRole.USER
        );
        AuthUser member = ensureUser(
                "kalfin-demo-member",
                "kalfin.demo.member@yomu.test",
                "Kalfin Demo Member",
                "KalfinDemo2!",
                AuthRole.USER
        );

        seedPrimaryDailyMissionIfMissing();
        seedDemoClanIfMissing(leader, member);
    }

    private boolean isDockerProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "docker".equalsIgnoreCase(profile));
    }

    private boolean isDemoSeedEnabled() {
        return environment.getProperty("app.demo.seed.enabled", Boolean.class, false);
    }

    private void seedReadingModule() {
        Category digitalLiteracy = getOrCreateCategory("Digital Literacy");
        Category newsMedia = getOrCreateCategory("News & Media");
        Category science = getOrCreateCategory("Science");

        seedTextIfMissing(
                "Mengenali Berita Palsu di Media Sosial",
                """
                Di era digital, informasi dapat menyebar dengan sangat cepat melalui media sosial.
                Namun, tidak semua informasi yang beredar dapat dipercaya. Berita palsu sering kali
                dibuat dengan judul sensasional agar menarik perhatian pembaca. Selain itu, berita palsu
                sering tidak mencantumkan sumber yang jelas atau menggunakan potongan informasi yang
                tidak lengkap. Oleh karena itu, pembaca perlu memeriksa sumber berita, tanggal publikasi,
                dan membandingkan informasi dengan media lain yang kredibel sebelum mempercayai atau
                membagikannya.
                """,
                digitalLiteracy,
                List.of(
                        new QuizSeed(
                            "Apa langkah penting sebelum membagikan informasi dari media sosial?",
                            "Langsung membagikannya jika judulnya menarik",
                            "Memeriksa sumber dan membandingkan dengan media kredibel",
                            "Mempercayai semua informasi yang viral",
                            "Mengabaikan tanggal publikasi berita",
                            "B"
                        ),
                        new QuizSeed(
                            "Mengapa judul sensasional perlu diwaspadai?",
                            "Karena selalu berasal dari sumber resmi",
                            "Karena biasanya tidak perlu diverifikasi",
                            "Karena dapat digunakan untuk menarik perhatian dan menyebarkan berita palsu",
                            "Karena pasti berisi informasi akademik",
                            "C"
                        ),
                        new QuizSeed(
                            "Apa ciri berita palsu yang disebutkan dalam teks?",
                            "Selalu memiliki data lengkap",
                            "Tidak mencantumkan sumber jelas atau menggunakan informasi tidak lengkap",
                            "Selalu ditulis oleh lembaga resmi",
                            "Selalu menggunakan bahasa ilmiah",
                            "B"
                        )
                )
        );

        seedTextIfMissing(
                "Pentingnya Membaca Berita dari Berbagai Sumber",
                """
                Membaca berita dari satu sumber saja dapat membuat seseorang memiliki pemahaman yang
                terbatas terhadap suatu peristiwa. Setiap media dapat memiliki sudut pandang, prioritas,
                dan gaya penyajian yang berbeda. Dengan membandingkan beberapa sumber, pembaca dapat
                melihat informasi secara lebih utuh dan mengurangi risiko terjebak pada bias tertentu.
                Kebiasaan ini penting untuk membangun kemampuan berpikir kritis, terutama ketika
                menghadapi isu publik yang kompleks.
                """,
                newsMedia,
                List.of(
                        new QuizSeed(
                            "Mengapa membaca berita dari satu sumber saja dapat bermasalah?",
                            "Karena semua media pasti salah",
                            "Karena pemahaman pembaca bisa menjadi terbatas",
                            "Karena berita tidak perlu dibandingkan",
                            "Karena sumber berita selalu memiliki isi yang sama",
                            "B"
                        ),
                        new QuizSeed(
                            "Apa manfaat membandingkan beberapa sumber berita?",
                            "Membuat pembaca tidak perlu berpikir kritis",
                            "Mengurangi risiko terjebak pada bias tertentu",
                            "Menghapus semua perbedaan sudut pandang",
                            "Membuat berita menjadi lebih pendek",
                            "B"
                        ),
                        new QuizSeed(
                            "Kemampuan apa yang dapat dibangun melalui kebiasaan membandingkan berita?",
                            "Kemampuan berpikir kritis",
                            "Kemampuan menghafal judul",
                            "Kemampuan membuat berita viral",
                            "Kemampuan menghindari membaca",
                            "A"
                        )
                )
        );

        seedTextIfMissing(
                "Mengapa Data Perlu Diperiksa Sebelum Dipercaya",
                """
                Data sering digunakan untuk memperkuat argumen dalam artikel, presentasi, maupun
                unggahan media sosial. Namun, data yang terlihat meyakinkan belum tentu akurat.
                Pembaca perlu memperhatikan sumber data, metode pengumpulan, tahun publikasi, dan
                konteks penggunaan data tersebut. Data lama atau data yang diambil dari konteks berbeda
                dapat menimbulkan kesimpulan yang keliru. Karena itu, memeriksa data merupakan bagian
                penting dari literasi informasi.
                """,
                science,
                List.of(
                        new QuizSeed(
                                "Mengapa data yang terlihat meyakinkan belum tentu akurat?",
                                "Karena semua data pasti palsu",
                                "Karena data tetap perlu diperiksa sumber, metode, dan konteksnya",
                                "Karena data tidak boleh dipakai dalam argumen",
                                "Karena data hanya berlaku untuk media sosial",
                                "B"
                        ),
                        new QuizSeed(
                                "Apa yang perlu diperhatikan saat memeriksa data?",
                                "Warna grafik saja",
                                "Jumlah komentar pembaca",
                                "Sumber data, metode pengumpulan, tahun publikasi, dan konteks",
                                "Jumlah emoji dalam unggahan",
                                "C"
                        ),
                        new QuizSeed(
                                "Apa akibat dari penggunaan data lama atau data di luar konteks?",
                                "Kesimpulan dapat menjadi keliru",
                                "Argumen otomatis menjadi benar",
                                "Data menjadi lebih kredibel",
                                "Pembaca tidak perlu melakukan verifikasi",
                                "A"
                        )
                )
        );
    }

    private Category getOrCreateCategory(String name) {
        Optional<Category> existingCategory = categoryRepository.findAll()
                .stream()
                .filter(category -> category.getName().equalsIgnoreCase(name))
                .findFirst();

        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }

        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private void seedTextIfMissing(String title, String content, Category category, List<QuizSeed> quizSeeds) {
        Optional<Text> existingText = textRepository.findAll()
            .stream()
            .filter(text -> text.getTitle().equalsIgnoreCase(title))
            .findFirst();

        if (existingText.isPresent()) {
            Text text = existingText.get();

            if (!text.isPublished()) {
                text.setPublished(true);
                textRepository.save(text);
            }

            if (questionRepository.findByTextId(text.getId()).isEmpty()) {
                quizSeeds.forEach(seed -> addQuestion(text, seed));
            }

            return;
        }

        Text text = new Text();
        text.setTitle(title);
        text.setContent(content);
        text.setCategory(category);
        text.setPublished(true);
        textRepository.save(text);

        quizSeeds.forEach(seed -> addQuestion(text, seed));
    }

    private void addQuestion(Text text, QuizSeed seed) {
        Question question = new Question();
        question.setText(text);
        question.setQuestion(seed.questionText());
        questionRepository.save(question);

        Option optionA = new Option();
        optionA.setQuestion(question);
        optionA.setText(seed.optionA());
        optionA.setCorrect("A".equals(seed.correctOption()));

        Option optionB = new Option();
        optionB.setQuestion(question);
        optionB.setText(seed.optionB());
        optionB.setCorrect("B".equals(seed.correctOption()));

        Option optionC = new Option();
        optionC.setQuestion(question);
        optionC.setText(seed.optionC());
        optionC.setCorrect("C".equals(seed.correctOption()));

        Option optionD = new Option();
        optionD.setQuestion(question);
        optionD.setText(seed.optionD());
        optionD.setCorrect("D".equals(seed.correctOption()));

        optionRepository.saveAll(List.of(optionA, optionB, optionC, optionD));
    }

    private AuthUser ensureUser(
            String username,
            String email,
            String displayName,
            String rawPassword,
            AuthRole role
    ) {
        return authRepository.findByEmail(email)
                .filter(AuthUser::isActive)
                .filter(user -> username.equals(user.getUsername()))
                .filter(user -> role == user.getRole())
                .orElseGet(() -> {
                    List<AuthUser> conflictingUsers = authRepository.findAllByEmailIgnoreCaseOrUsernameIgnoreCase(email, username);
                    if (!conflictingUsers.isEmpty()) {
                        authRepository.deleteAll(conflictingUsers);
                    }
                    return authRepository.save(new AuthUser(
                            username,
                            email,
                            null,
                            displayName,
                            passwordEncoder.encode(rawPassword),
                            role
                    ));
                });
    }

    private void seedPrimaryDailyMissionIfMissing() {
        LocalDate today = LocalDate.now();
        dailyMissionRepository.findByActiveDateAndPrimaryTrue(today).orElseGet(() -> dailyMissionRepository.save(
                DailyMission.builder()
                        .title("Complete 1 reading today")
                        .targetCount(1)
                        .activeDate(today)
                        .primary(true)
                        .build()
        ));
    }

    private void seedDemoClanIfMissing(AuthUser leader, AuthUser member) {
        if (clanRepository.existsByNameIgnoreCase("Kalfin Demo Clan")) {
            return;
        }

        Tier bronze = tierRepository.findByCode(TierCode.BRONZE)
                .orElseGet(() -> tierRepository.save(new Tier(TierCode.BRONZE, "Bronze")));

        Clan clan = clanRepository.save(new Clan("Kalfin Demo Clan", bronze, leader.getId()));
        ClanMember leaderMember = clanMemberRepository.save(new ClanMember(clan, leader.getId(), ClanMemberRole.LEADER));
        ClanMember memberEntry = clanMemberRepository.save(new ClanMember(clan, member.getId(), ClanMemberRole.MEMBER));
        clan.addMember(leaderMember);
        clan.addMember(memberEntry);
        clanRepository.save(clan);
    }

    private record QuizSeed(
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption
    ) {}
}
