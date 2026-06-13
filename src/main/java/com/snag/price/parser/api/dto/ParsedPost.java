package com.snag.price.parser.api.dto;

/**
 * Общий контракт для любого спарсенного поста.
 * VkPost, MagnitPost, LentaPost — все реализуют этот интерфейс.
 */
public interface ParsedPost {
    // Уникальный ID в системе источника
    String getExternalId();

    // Название / заголовок
    String getProductName();

    // Полный текст
    String getText();

    // URL картинки
    String getImageUrl();

    // Ссылка на источник
    String getSourceUrl();

    // Промокод (может быть null)
    String getPromoCode();

    // Скидка "-30%" (может быть null)
    String getDiscountAmount();

    // Название источника "VK_PROBNICK", "MAGNIT", "LENTA"
    String getSourceName();

    // Дата публикации (unix timestamp)
    long getDate();
}
