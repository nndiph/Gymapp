package com.nndiph.liftverse.feature.summary

// Route of "Selesai sesi". Session ids are UUIDs, so they need no URL encoding.

const val SESSION_ID_ARG = "sessionId"
private const val SESSION_SUMMARY_BASE = "session_summary"
const val SESSION_SUMMARY_ROUTE = "$SESSION_SUMMARY_BASE/{$SESSION_ID_ARG}"

fun sessionSummaryRoute(sessionId: String): String = "$SESSION_SUMMARY_BASE/$sessionId"
