-- Usage: redis-cli --eval <this-file> <one-invite-code-qr-key> , check|apply
-- Run during the coordinated service upgrade; check is the default and is read-only.
-- Explicit keys only. This script never scans Redis, deletes data or changes TTLs.
if #KEYS ~= 1 then
    return redis.error_reply('Exactly one inventoried invite-code QR cache key is required')
end
local mode = ARGV[1] or 'check'
if mode ~= 'check' and mode ~= 'apply' then
    return redis.error_reply('Mode must be check or apply')
end
local key = KEYS[1]
local marker = 'global:wechat:miniprogram:invite_code_qr:'
local start = string.find(key, marker, 1, true)
if not start or (start ~= 1 and string.sub(key, start - 1, start - 1) ~= ':') then
    return redis.error_reply('Key is not an invite-code QR cache key')
end
local kind = redis.call('TYPE', key).ok
if kind == 'none' then
    return 'absent'
end
if kind ~= 'string' then
    return redis.error_reply('Expected a string cache value')
end
local value = redis.call('GET', key)
local valid = pcall(cjson.decode, value)
if not valid then
    return redis.error_reply('Cache value is not JSON; left unchanged')
end
local oldType = '"com.ym.agriculture.employee.model.vo.MiniProgramCodeVo"'
local newType = '"com.ym.agriculture.farmtask.employee.model.vo.MiniProgramCodeVo"'
local pattern = string.gsub(oldType, '(%W)', '%%%1')
local updated, replacements = string.gsub(value, pattern, newType)
if replacements == 0 then
    return 'unchanged'
end
if replacements ~= 1 then
    return redis.error_reply('Ambiguous type metadata; left unchanged')
end
if mode == 'check' then
    return 'migration-required'
end
redis.call('SET', key, updated, 'KEEPTTL')
return 'migrated'
