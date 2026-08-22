/*
Base : https://play.google.com/store/apps/details?id=com.openai.chatgpt
Author : Geno
WhatsApp Channel: https://whatsapp.com/channel/0029Vb6hVYK8V0tkiz4bKs0N
Featured 
Support Streaming Chat
Support Conversation
Support WebSearch
*/

const axios = require('axios')
const crypto = require('crypto')

const parseCookies = (arr) => Object.fromEntries((arr || []).map(c => c.split(';')[0].split('=').map(s => s.trim())))

function cleanSpecialTags(text) {
  if (!text) return '';
  text = text.replace(/\ue200entity\ue202([^\ue201]+)\ue201/g, (match, p1) => {
    try {
      const arr = JSON.parse(p1);
      return arr[1] || arr[0] || '';
    } catch {
      return '';
    }
  });
  text = text.replace(/\ue200[^\ue201]*\ue201/g, '');
  return text.trim();
}

async function getSession() {
  const deviceId = crypto.randomUUID()
  const res = await axios.post('https://android.chat.openai.com/backend-anon/sentinel/chat-requirements', {}, {
    headers: {
      'User-Agent': 'ChatGPT/1.2026.181 (Android 16; Neo/1.0; build 2222222)',
      'OAI-Package-Name': 'com.openai.chatgpt',
      'OAI-Client-Type': 'android',
      'OAI-Device-Id': deviceId,
      'Accept-Language': 'id-ID,in;q=0.9',
      'X-Device-Tier': 'upper_mid',
      'X-OpenAI-Target-Path': '/backend-anon/sentinel/chat-requirements',
      'ChatGPT-Account-Id': 'default',
      'ChatGPT-Residency-Region': 'no_constraint',
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    }
  })

  const cookies = parseCookies(res.headers['set-cookie'])
  const cookieStr = Object.entries(cookies).map(([k, v]) => `${k}=${v}`).join('; ')
  
  let oaiSc = cookies['oai-sc']
  if (!oaiSc && res.data?.token) {
    oaiSc = `0${res.data.token}`
  }

  const cookie = oaiSc && !cookieStr.includes('oai-sc') ? `oai-sc=${oaiSc}; ${cookieStr}` : cookieStr

  return { cookie, deviceId, parentMessageId: crypto.randomUUID() }
}

async function chatgpt(prompt, auth = null, chatId = null) {
  auth = auth || await getSession()
  if (!auth.deviceId) auth.deviceId = crypto.randomUUID()
  if (!auth.parentMessageId) auth.parentMessageId = crypto.randomUUID()

  const isAuthorized = !!(auth.authorization || auth.token)
  const baseUrl = isAuthorized ? 'https://android.chat.openai.com/backend-api' : 'https://android.chat.openai.com/backend-anon'
  
  const currentMessageId = crypto.randomUUID()
  const parentMessageId = auth.parentMessageId

  const headers = {
    'User-Agent': 'ChatGPT/1.2026.181 (Android 16; Neo/1.0; build 2222222)',
    'OAI-Package-Name': 'com.openai.chatgpt',
    'OAI-Client-Type': 'android',
    'OAI-Device-Id': auth.deviceId,
    'Accept-Language': 'id-ID,in;q=0.9',
    'X-Device-Tier': 'upper_mid',
    'X-OpenAI-Target-Path': isAuthorized ? '/backend-api/f/conversation' : '/backend-anon/f/conversation',
    'ChatGPT-Account-Id': isAuthorized ? (auth.accountId || 'default') : 'default',
    'ChatGPT-Residency-Region': 'no_constraint',
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    'Cookie': auth.cookie,
    'X-Sentinel-Payload': JSON.stringify({
      bot_token: {
        failure_reason: "-2: Standard Integrity API error (-2): The Play Store app is either not installed or not the official version.\nAsk the user to install an official and recent version of Play Store.\n (https://developer.android.com/google/play/integrity/reference/com/google/android/play/core/integrity/model/StandardIntegrityErrorCode.html#PLAY_STORE_NOT_FOUND).",
        failure_detail: "[qdb0.j(SourceFile:9), g4n.a(SourceFile:85), f4n.invokeSuspend(SourceFile:14), kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(SourceFile:5), qni.run(SourceFile:104), fnf.run(SourceFile:112)]"
      }
    })
  }

  if (isAuthorized) {
    headers['Authorization'] = auth.authorization || `Bearer ${auth.token}`
  }

  const userMessage = {
    id: currentMessageId,
    author: { role: "user" },
    content: {
      content_type: "text",
      parts: [prompt]
    },
    status: "finished_successfully",
    recipient: "all"
  }

  const body = {
    action: "next",
    messages: [userMessage],
    model: "auto",
    history_and_training_disabled: false,
    fork_from_shared_post: false,
    enable_message_followups: true,
    force_use_sse: true,
    force_use_search: null,
    force_paragen: false,
    supported_encodings: ["v1"],
    supports_buffering: true,
    timezone: "Asia/Makassar",
    timezone_offset_min: -480,
    system_hints: [],
    is_onboarding_conversation: false,
    no_auth_ad_preferences: {
      personalization_enabled: true,
      history_enabled: true
    },
    client_prepare_state: "none",
    stream: true
  }

  if (chatId) {
    body.conversation_id = chatId
    body.parent_message_id = parentMessageId
  }

  const stream = await axios.post(`${baseUrl}/f/conversation`, body, {
    headers,
    responseType: 'stream'
  })

  return new Promise((resolve) => {
    let text = '', buf = ''
    let lastPath = null
    let lastOp = null
    let finalChatId = chatId
    let currentAssistantMsgId = null

    stream.data.on('data', chunk => {
      buf += chunk.toString()
      const lines = buf.split('\n')
      buf = lines.pop()

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || trimmed === 'data: [DONE]') continue

        if (trimmed.startsWith('data: ')) {
          try {
            const data = JSON.parse(trimmed.substring(6))

            if (data.conversation_id) {
              finalChatId = data.conversation_id
            }

            const p = data.p !== undefined ? data.p : lastPath
            const o = data.o !== undefined ? data.o : lastOp

            if (data.p !== undefined) lastPath = data.p
            if (data.o !== undefined) lastOp = data.o

            if (o === 'add' && data.v && data.v.message) {
              if (data.v.message.author && data.v.message.author.role === 'assistant') {
                currentAssistantMsgId = data.v.message.id
                const parts = data.v.message.content?.parts
                if (parts && parts[0]) {
                  text = parts[0]
                }
              }
            } else if (o === 'patch' && Array.isArray(data.v)) {
              for (const op of data.v) {
                if (op.o === 'append' && op.p && op.p.startsWith('/message/content/parts/')) {
                  text += op.v
                }
              }
            } else if (o === 'append' && p && p.startsWith('/message/content/parts/') && typeof data.v === 'string') {
              text += data.v
            }
          } catch {}
        }
      }
    })

    stream.data.on('end', () => {
      if (currentAssistantMsgId) {
        auth.parentMessageId = currentAssistantMsgId
      }
      resolve(JSON.stringify({ response: cleanSpecialTags(text), chatId: finalChatId, auth }, null, 2))
    })
  })
}

// USAGE
if (require.main === module) {
  async function run() {
    console.log("-> Pesan 1...")
    const r1 = await chatgpt('Hai, nama aku geno.')
    const j1 = JSON.parse(r1)
    console.log("Bot:", j1.response)
const promptss = `
tolong terjemahkan kebahasa Indonesia 
"DO NOT REPOST!",
    "IF YOU ENJOYED THIS SERIES AND WANT A FASTER RELEASE",
    "PLEASE READ ON MANGAGO.ME",
    "I SHOULDN'T HAVE TURNED AWAY FROM YOU.",
    "EVEN IF I DIDN'T KNOW, I SHOULDN'T HAVE DONE THAT TO YOU.",
    "THEN I WOULD NEVER HAVE HURT SOMEONE...",
    "...WHO'S SO AFRAID OF BEING ABANDONED...",
    "I QUIETLY WATCHED SHINWOO'S SLEEPING FACE...",
    "...WHILE I THOUGHT OF THOSE THINGS.",
    "I'M SORRY...",
    "I HOPED THAT I COULD SOON PROVIDE A COVER OF WARMTH...",
    "...OVER HIS PALE FACE.",
    "SOMEONE LIKE YOU",
    "COMIC EUNEUN ORIGINAL NOVEL KIE",
    "YOU UP?",
    "YEAH... WHEN DID YOU GET HERE?",
    "I JUST ARRIVED.",
    "YOU WERE SLEEPING SO SOUNDLY, ARE YOU FEELING OKAY?",
    "AMI OKAY...?",
    "WHY...",
    "OH... I'M FINE.",
    "DID YOU EAT?",
    "...NOT YET.",
    "THEN LET'S EAT NOW.",
    "OH, AND YOUR THINGS WILL BE HERE SOON,",
    "HUH?",
    "I TOLD THEM TO BRING ALL YOUR THINGS TO THE ANNEX,",
    "AND I JUST TOOK YOUR KEY. SORRY.",
    "HAN, THAT'S...",
    "I KNOW THIS IS ALL VERY ONE-SIDED, BUT I DON'T WANT TO LEAVE YOU THERE BY YOURSELF.",
    "I FEEL LIKE YOU'LL JUST RUN AWAY ONE DAY,",
    "DO YOU KNOW WHAT HAN'S FAVORITE STORY WAS WHEN HE WAS YOUNGER?",
    "IT WAS THE FAIRY AND THE WOTE.",
    "WHEN HE WAS YOUNG, HE WOULD ALWAYS SAY THAT WHEN HEE T T T LOF HIS LIFE, HE WOULD BURN ALL THEIR CLOTHES AND SHOES.",
    "YOU MET UP WITH JEONGHYEON?",
    "YES, FOR A MINUTE.",
    "THERE WAS SOMETHINGWE NEEDED TO DISCUSS. HE'S GOTTEN REALLY GOOD AT CHEWING PEOPLE OUT,",
    "HIS PARENTS AREN'T LIKE THAT AT ALL, I WONDER WHERE HE GETS IT FROM.",
    "WHAT DID JEONGHYEON SAY?",
    "HE SAID THAT IF WE BREAK UP, HE WOULD OBLITERATE ME.",
    "DON'T MIND HIM. HE'S PROBABLY JUST WORRIED.",
    "SIT",
    "I DO MIND.",
    "IT'S A WARNING FROM MY OLDEST FRIEND.",
    "IT'S TOO HOT HERE, LET'S GO INSIDE.",
    "YEAH.",
    "DROP XOF",
    "WAIT, THAT CANDY...",
    "I SAW YOUR GRANDPA EARLIER WHEN HE STOPPED BY. HE GAVE ME THESE BEFORE HE LEFT.",
    "HE GAVE YOU CANDY?",
    "YEAH.",
    "SO, THAT'S HOW HE'S GOING TO DO THINGS, HUH?",
    "WHY? SHOULD I NOT HAVE TAKEN THEM?",
    "NO, IT'S NOT THAT...",
    "WHAT ARE THESE CANDIES, ANYWAY?",
    "THEY'RE JUST REGULAR CANDY. GRANDMA USED TO EAT A LOT OF THEM WHEN SHE WAS ALIVE.",
    "GRANDPA NEVER GIVES THEM TO OTHER PEOPLE,",
    "HE DOESN'T EVEN GIVE THEM TO ME, HE MUST HAVE REALLY TAKEN A LIKING TO YOU.",
    "HUNNH",
    "SEND IT TO YOU.",
    "BUDDY",
    "MYEONGJIN KIM",`   
    console.log("\n-> Pesan 2 (Test Converstion)...")
    const r2 = await chatgpt(promptss, j1.auth, j1.chatId)
    const j2 = JSON.parse(r2)
    console.log("Bot:", j2.response)
/*
    console.log("\n-> Pesan 3 (Test Web Search)...")
    const r3 = await chatgpt('Siapa juara Euro 2024?', j2.auth, j2.chatId)
    const j3 = JSON.parse(r3)
    console.log("Bot:", j3.response)
    */
  }

  run()
}
