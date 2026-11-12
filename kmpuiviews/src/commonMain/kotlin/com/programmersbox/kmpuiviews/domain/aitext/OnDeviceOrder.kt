package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId

/** Engines AiText may use on this platform. Never includes Provider or Template. */
expect val onDeviceAiOrder: List<SummaryEngineId>
