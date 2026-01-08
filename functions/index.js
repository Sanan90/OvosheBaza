const { onCall, onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const axios = require("axios");
const { defineSecret, defineString } = require("firebase-functions/params");
const admin = require("firebase-admin");

const TELEGRAM_TOKEN = defineSecret("TELEGRAM_TOKEN");
const TELEGRAM_CHAT_ID = defineString("TELEGRAM_CHAT_ID");
const TELEGRAM_WEBHOOK_SECRET = defineSecret("TELEGRAM_WEBHOOK_SECRET");

admin.initializeApp();

// Красивое число: убираем 0.30000000004 и лишние нули
function fmtNum(n, digits = 3) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  const fixed = x.toFixed(digits);
  return fixed.replace(/\.?0+$/, ""); // "1.000" -> "1", "0.500"->"0.5"
}

function fmtMoney(n) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  // можно без копеек:
  return Math.round(x).toString();
}

exports.sendOrderToTelegram = onCall(
  {
    cors: true,
    secrets: [TELEGRAM_TOKEN],
  },
  async (request) => {
    try {
      const data = request.data || {};
      const type = String(data.type || "ORDER").toUpperCase();

      const token = TELEGRAM_TOKEN.value();
      const chatId = TELEGRAM_CHAT_ID.value();

      if (!token) throw new Error("TELEGRAM_TOKEN пустой");
      if (!chatId) throw new Error("TELEGRAM_CHAT_ID пустой");

      let text = "";

      // =========================
      // 1) SUPPORT (поддержка)
      // =========================
      if (type === "SUPPORT") {
        const phone = (data.phone || "").toString().trim();
        const question = (data.question || "").toString().trim();

        if (!question) throw new Error("В поддержке нет question");

        text =
          `🆘 ОБРАЩЕНИЕ В ПОДДЕРЖКУ\n\n` +
          `📞 Телефон: ${phone || "-"}\n\n` +
          `💬 Вопрос:\n${question}\n`;

      // =========================
      // 2) REQUEST (заявка на товар)
      // =========================
      } else if (type === "REQUEST") {
        const customerName = (data.customerName || "").toString().trim();
        const customerPhone = (data.customerPhone || "").toString().trim();
        const requestedProduct = (data.requestedProduct || "").toString().trim();
        const requestedQuantity = (data.requestedQuantity || "").toString().trim();
        const comment = (data.comment || "").toString().trim();

        if (!requestedProduct) throw new Error("В заявке нет requestedProduct");
        if (!customerPhone && !customerName) throw new Error("В заявке нет контактов (имя/телефон)");

        text =
          `📝 ЗАЯВКА НА ТОВАР\n\n` +
          `👤 Имя: ${customerName || "-"}\n` +
          `📞 Телефон: ${customerPhone || "-"}\n\n` +
          `🛒 Что нужно заказать:\n${requestedProduct}\n\n` +
          `⚖️ Количество:\n${requestedQuantity || "-"}\n` +
          (comment ? `\n💬 Комментарий:\n${comment}\n` : "");

      // =========================
      // 3) ORDER (заказ из корзины)
      // =========================
      } else {
        const items = data.items;

        if (!Array.isArray(items) || items.length === 0) {
          throw new Error("В заказе нет items (пусто или не массив)");
        }

        const uid = (data.uid || "").toString().trim();
                const orderId = (data.orderId || "").toString().trim();
                if (!uid || !orderId) {
                  throw new Error("В заказе нет uid/orderId для кнопок");
                }


        const customerName = (data.customerName || "").toString().trim();
        const customerPhone = (data.customerPhone || "").toString().trim();
        const customerAddress = (data.customerAddress || "").toString().trim();
        const comment = (data.comment || "").toString().trim();

        // Красивый список как чек
        const itemsText = items
          .map((it) => {
            const name = (it.name || "").toString();
            const qty = it.quantity;
            const unit = (it.unit || "").toString().toUpperCase(); // "KG" или "PIECE"
            const price = it.price;
            const sum = it.sum;

            const unitLabel = unit === "KG" ? "кг" : "шт";

            return `• ${name} — ${fmtNum(qty)} ${unitLabel} × ${fmtMoney(price)} = ${fmtMoney(sum)}`;
          })
          .join("\n");

        const total = fmtMoney(data.total);
        const subtotal = fmtMoney(data.subtotal);
        const deliveryFee = fmtMoney(data.deliveryFee);
        const discount = fmtMoney(data.discount);

        text =
          `🧾 НОВЫЙ ЗАКАЗ\n\n` +
          `👤 Имя: ${customerName || "-"}\n` +
          `📞 Телефон: ${customerPhone || "-"}\n` +
          `📍 Адрес: ${customerAddress || "-"}\n` +
          (comment ? `💬 Комментарий: ${comment}\n` : "") +
          `\n🛒 Товары:\n${itemsText}\n\n` +
          `💵 Подитог: ${subtotal}\n` +
          `🚚 Доставка: ${deliveryFee}\n` +
          `🏷 Скидка: ${discount}\n` +
           `💰 ИТОГО: ${total}\n\n` +
                    `Статус: Получен`;

                  data._replyMarkup = {
                    inline_keyboard: [
                      [
                        { text: "✅ Принять", callback_data: `ACCEPT|${uid}|${orderId}` },
                        { text: "❌ Отменить", callback_data: `CANCEL|${uid}|${orderId}` },
                      ],
                    ],
                  };
      }

      const url = `https://api.telegram.org/bot${token}/sendMessage`;

      const payload = {
        chat_id: chatId,
        text,
      ...(data._replyMarkup ? { reply_markup: data._replyMarkup } : {}),
            };

            const tgResp = await axios.post(url, payload);

      logger.info("Telegram sent ok", tgResp.data);
      return { ok: true };
    } catch (e) {
      logger.error("sendOrderToTelegram error", e);
      throw new Error(e.message || String(e));
    }
  }
);

function baseOrderText(text) {
  const lines = String(text || "").split("\n");
  const filtered = lines.filter(
    (line) =>
      !line.startsWith("Статус:") &&
      line.trim() !== "✅ Завершён" &&
      line.trim() !== "❌ Заказ отменён"
  );
  return filtered.join("\n").trim();
}

function orderStatusLabel(status) {
  switch (status) {
    case "ACCEPTED":
      return "Принят / собирается";
    case "IN_TRANSIT":
      return "В пути";
    case "DONE":
      return "✅ Завершён";
    case "CANCELLED":
      return "❌ Заказ отменён";
    default:
      return "Получен";
  }
}

function orderStatusPushLabel(status) {
  switch (status) {
    case "RECEIVED":
      return "получен";
    case "ACCEPTED":
      return "принят и собирается";
    case "IN_TRANSIT":
      return "в пути";
    case "DONE":
      return "завершён";
    case "CANCELLED":
      return "заказ отменён";
    default:
      return "получен";
  }
}

async function sendStatusPush(db, uid, orderId, status) {
  const userSnap = await db.collection("users").doc(uid).get();
  if (!userSnap.exists) return;
  const tokens = userSnap.get("fcmTokens");
  if (!Array.isArray(tokens) || tokens.length === 0) return;

  const label = orderStatusPushLabel(status);
  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: "Статус заказа изменён",
      body: `Заказ №${orderId}: ${label}`,
    },
  });

  const invalidTokens = [];
  response.responses.forEach((resp, index) => {
    if (resp.success) return;
    const code = resp.error?.code;
    if (
      code === "messaging/invalid-registration-token" ||
      code === "messaging/registration-token-not-registered"
    ) {
      invalidTokens.push(tokens[index]);
    }
  });

  if (invalidTokens.length > 0) {
    await db.collection("users").doc(uid).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
    });
  }
}


function replyMarkupForStatus(status, uid, orderId) {
  switch (status) {
    case "ACCEPTED":
      return {
        inline_keyboard: [
          [
            { text: "🚚 Заказ в пути", callback_data: `IN_TRANSIT|${uid}|${orderId}` },
            { text: "❌ Отменить", callback_data: `CANCEL|${uid}|${orderId}` },
          ],
        ],
      };
    case "IN_TRANSIT":
      return {
        inline_keyboard: [
          [
            { text: "✅ Завершён", callback_data: `DONE|${uid}|${orderId}` },
            { text: "❌ Отменить", callback_data: `CANCEL|${uid}|${orderId}` },
          ],
        ],
      };
    default:
      return { inline_keyboard: [] };
  }
}

async function answerCallbackQuery(token, callbackId, text) {
  if (!callbackId) return;
  const url = `https://api.telegram.org/bot${token}/answerCallbackQuery`;
  await axios.post(url, {
    callback_query_id: callbackId,
    text,
    show_alert: false,
  });
}

async function editMessageText(token, chatId, messageId, text, replyMarkup) {
  const url = `https://api.telegram.org/bot${token}/editMessageText`;
  await axios.post(url, {
    chat_id: chatId,
    message_id: messageId,
    text,
    ...(replyMarkup ? { reply_markup: replyMarkup } : {}),
  });
}

exports.telegramOrderStatusWebhook = onRequest(
  {
    secrets: [TELEGRAM_TOKEN, TELEGRAM_WEBHOOK_SECRET],
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    try {
      const secret = TELEGRAM_WEBHOOK_SECRET.value();
      if (
        secret &&
        req.get("x-telegram-bot-api-secret-token") !== secret
      ) {
        res.status(403).send("Forbidden");
        return;
      }

      const body = req.body || {};
      const callback = body.callback_query;
      if (!callback) {
        res.status(200).send("ok");
        return;
      }

      const token = TELEGRAM_TOKEN.value();
      const chatId = callback.message?.chat?.id;
      if (!token) throw new Error("TELEGRAM_TOKEN пустой");

      if (String(chatId) !== String(TELEGRAM_CHAT_ID.value())) {
        await answerCallbackQuery(token, callback.id, "Недоступно в этом чате");
        res.status(200).send("ok");
        return;
      }

      const data = (callback.data || "").toString();
      const [action, uid, orderId] = data.split("|");
      if (!action || !uid || !orderId) {
        await answerCallbackQuery(token, callback.id, "Некорректные данные");
        res.status(200).send("ok");
        return;
      }

      const db = admin.firestore();
      const orderRef = db.collection("users").doc(uid).collection("orders").doc(orderId);
      const snapshot = await orderRef.get();

      if (!snapshot.exists) {
        await answerCallbackQuery(token, callback.id, "Заказ не найден");
        res.status(200).send("ok");
        return;
      }

      const currentStatus = snapshot.get("status") || "RECEIVED";
      if (currentStatus === "DONE") {
        await answerCallbackQuery(token, callback.id, "Заказ уже завершён");
        res.status(200).send("ok");
        return;
      }

      const now = Date.now();
      const messageText = callback.message?.text || "";
      const baseText = baseOrderText(messageText);
      const messageId = callback.message?.message_id;

      switch (action) {
        case "ACCEPT": {
          await orderRef.update({ status: "ACCEPTED", statusUpdatedAt: now });
          const text = `${baseText}\n\nСтатус: ${orderStatusLabel("ACCEPTED")}`;
          await editMessageText(
            token,
            chatId,
            messageId,
            text,
            replyMarkupForStatus("ACCEPTED", uid, orderId)
          );
          await sendStatusPush(db, uid, orderId, "ACCEPTED");
          await answerCallbackQuery(token, callback.id, "Заказ принят");
          break;
        }
        case "IN_TRANSIT": {
          await orderRef.update({ status: "IN_TRANSIT", statusUpdatedAt: now });
          const text = `${baseText}\n\nСтатус: ${orderStatusLabel("IN_TRANSIT")}`;
          await editMessageText(
            token,
            chatId,
            messageId,
            text,
            replyMarkupForStatus("IN_TRANSIT", uid, orderId)
          );
          await sendStatusPush(db, uid, orderId, "IN_TRANSIT");
          await answerCallbackQuery(token, callback.id, "Заказ в пути");
          break;
        }
        case "DONE": {
          await orderRef.update({ status: "DONE", statusUpdatedAt: now });
          const text = `${baseText}\n\n✅ Завершён`;
          await editMessageText(token, chatId, messageId, text, { inline_keyboard: [] });
          await sendStatusPush(db, uid, orderId, "DONE");
          await answerCallbackQuery(token, callback.id, "Заказ завершён");
          break;
        }
        case "CANCEL": {
          await orderRef.delete();
          await sendStatusPush(db, uid, orderId, "CANCELLED");
          try {
            await db.collection("orders").doc(orderId).delete();
          } catch (e) {
            logger.warn("Failed to delete from global orders collection", e);
          }
          const text = `${baseText}\n\n❌ Заказ отменён`;
          await editMessageText(token, chatId, messageId, text, { inline_keyboard: [] });
          await answerCallbackQuery(token, callback.id, "Заказ отменён");
          break;
        }
        default: {
          await answerCallbackQuery(token, callback.id, "Неизвестное действие");
        }
      }

      res.status(200).send("ok");
    } catch (e) {
      logger.error("telegramOrderStatusWebhook error", e);
      res.status(500).send("error");
    }
  }
);