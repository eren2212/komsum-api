package com.ereniridere.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ereniridere.entity.Badge;
import com.ereniridere.entity.TaskDefinition;
import com.ereniridere.entity.enums.TaskType;
import com.ereniridere.repository.BadgeRepository;
import com.ereniridere.repository.TaskDefinitionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Komşu Görevi sisteminin sabit tanımlarını (görev tipleri ve rozet eşikleri)
 * uygulama açılışında ekler. Idempotenttir — kayıt zaten varsa dokunmaz, böylece
 * puan değerleri DB üzerinden ayarlandığında yeniden başlatma bunları ezmez.
 */
@Component
@RequiredArgsConstructor
public class GamificationSeeder implements CommandLineRunner {

	private final TaskDefinitionRepository taskDefinitionRepository;
	private final BadgeRepository badgeRepository;

	@Override
	public void run(String... args) {
		seedTask(TaskType.CREATE_POST, "Mahallene bir şey paylaş",
				"Bugün akışa bir gönderi paylaş, komşuların haberdar olsun.", 10);
		seedTask(TaskType.JOIN_EVENT, "Bir etkinliğe katıl",
				"Mahallendeki bir etkinliğe katılarak komşularınla buluş.", 15);
		seedTask(TaskType.WRITE_COMMENT, "Bir komşuna yorum yaz",
				"Bir gönderiye yorum yazarak sohbete katıl.", 5);

		seedBadge("Yeni Komşu", "İlk adımları attın, aramıza hoş geldin!", 50);
		seedBadge("Mahalle Sakini", "Mahalleni canlı tutuyorsun.", 150);
		seedBadge("Mahallenin Yıldızı", "Komşuların seni tanıyor!", 400);
	}

	private void seedTask(TaskType type, String title, String description, int points) {
		if (!taskDefinitionRepository.existsByType(type)) {
			taskDefinitionRepository.save(TaskDefinition.builder()
					.type(type)
					.title(title)
					.description(description)
					.pointsValue(points)
					.active(true)
					.build());
		}
	}

	private void seedBadge(String name, String description, int threshold) {
		if (!badgeRepository.existsByName(name)) {
			badgeRepository.save(Badge.builder()
					.name(name)
					.description(description)
					.pointThreshold(threshold)
					.build());
		}
	}
}
