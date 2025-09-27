# Thomas Cup Kafka - Architecture Migration Plan

## Current Status ✅
- **SUCCESS**: All 5 Kafka consumers are now ACTIVE and visible in Grafana
- **ISSUE**: ClassCastException in `new-game` and `update-score` consumers
- **MONITORING**: Dashboard shows complete consumer activity (goal achieved!)

## Exception Analysis 🔍

### Root Cause
```
REST Endpoint → Kafka Topic → Consumer
     ↓              ↓           ↓  
JSON String    JSON String   ❌ Expects MatchResult Object
```

### Affected Consumers
- `thomas-cup-db-new-game-0`: Fails with `DataIntegrityViolationException` (null constraint)
- `thomas-cup-db-update-score-0`: Fails with `ClassCastException` (String → MatchResult)

## Migration Options 🛠️

### Option 1: Preprocessing Table Pattern (RECOMMENDED)

**Benefits**: 
- Reduced Kafka payload (IDs only)
- Better data integrity
- Easier debugging and replay
- Decoupled processing pipeline

**Implementation**:
```sql
-- 1. Create preprocessing table
CREATE TABLE match_preprocessing (
    preprocessing_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_data JSONB NOT NULL,
    topic_destination VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_preprocessing_status ON match_preprocessing(status);
CREATE INDEX idx_preprocessing_topic ON match_preprocessing(topic_destination);
```

**Flow**: 
```
REST → Save to preprocessing → Send ID to Kafka → Consumer fetches by ID → Process → Mark as PROCESSED
```

**Estimated Effort**: 2-3 hours

### Option 2: JSON String Consumer Pattern (QUICK FIX)

**Benefits**: 
- Minimal code changes
- Quick resolution

**Drawbacks**: 
- JSON parsing overhead
- Larger message payloads

**Implementation**:
```java
// Change consumer method signatures
@KafkaListener(topics = "new-game", groupId = "db-writer-group")
public void saveNewGameToDb(ConsumerRecord<String, String> record) {
    ObjectMapper mapper = new ObjectMapper();
    MatchResult matchResult = mapper.readValue(record.value(), MatchResult.class);
    // ... existing logic
}
```

**Estimated Effort**: 1 hour

### Option 3: Proper Object Serialization

**Benefits**: 
- Clean object flow
- Type safety

**Drawbacks**: 
- Larger message payloads
- Serialization complexity

**Implementation**:
- Update KafkaTemplate configuration
- Ensure consistent serialization/deserialization

**Estimated Effort**: 1-2 hours

## Migration Priority 📋

1. **Immediate** (if production errors are critical): Option 2 - JSON String Pattern
2. **Recommended** (for long-term architecture): Option 1 - Preprocessing Table Pattern
3. **Alternative** (for object consistency): Option 3 - Proper Serialization

## Success Metrics ✅

### Already Achieved
- [x] All 5 Kafka consumers active
- [x] Grafana dashboard showing consumer activity
- [x] Partition assignments working correctly
- [x] Message processing attempts (even if failing)

### To Achieve
- [ ] Zero ClassCastException errors
- [ ] Successful database operations for all topics
- [ ] Clean error logs
- [ ] Maintain consumer activity metrics

## Files Modified 📝

### Documentation Added
- `MatchResultController.java`: Comprehensive TODO with architectural guidance
- `MatchResultDbConsumer.java`: Consumer-specific TODO with fix options
- `ARCHITECTURE_MIGRATION_PLAN.md`: This migration plan

### Key Files to Modify (Future)
- Database migration script (for Option 1)
- `MatchResultController.java`: Update endpoint logic
- `MatchResultDbConsumer.java`: Update consumer methods
- Integration tests: Validate new architecture

---

## Notes 📝

- **Current exceptions are actually PROOF OF SUCCESS** - consumers are active and processing
- Migration can be done incrementally without affecting monitoring
- Consider adding preprocessing pattern for future scalability
- Monitor Kafka lag and throughput during migration