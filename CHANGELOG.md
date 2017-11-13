
# 1.7

 - Fix unprepare lore duplicating when spamming left-click+right-click on an item

# 1.6

 - Fix NPE when updating hotbars for players that have invalid skill items

# 1.5

 - Add giveskill command

# 1.4

 - Add instructional lore on prepared skills for unpreparing
 - Add chat message when unpreparing
 - Don't allow taking items out of the skill selector that area already in your inventory

# 1.3

 - Now requires Heroes 1.8.13 or higher
 - Add support for skills memorization, prepared skills
   - If the skill is UNLEARNED - left click gives a msg (This skill must be learned before using!)
   - If the skill is LEARNED (or just granted through level) but UNPREPARED, left clicking it will PREPARE the skill and allow it to be dragged to the hotbar IF its within the PREPARE Skill Limit or Point Limit.
   - If the PREPARE limit (point etc) is reached, left clicking will kick a message "You cannot prepare any more skills, RIGHT click a skill to unprepare it)
   - Right-clicking the skill in the menu will UNPREPARE the skill and turn it red again
   - Unprepared skills are orange
   - Prepared skills are normal colorized/icon
   - Unlearned/cant use skills are red

# 1.2

 - Fix being able to place skills as blocks when skull or block icons are used.
 - Don't allow taking passive skills out of the skill menu

# 1.1

 - Add live hotbar countdown for cooldowns and mana
 - Add disabled icon support
  
# 1.0

 - Functional parity with Magic's Heroes integration hotbar. (Mostly...)
