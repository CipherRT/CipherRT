# name: (group, equipment, primary, secondary, steps, mistakes)
L = {}
def e(name, group, equip, primary, secondary, steps, mistakes):
    assert name not in L, name
    L[name] = dict(group=group, equipment=equip, primary=primary, secondary=secondary, steps=steps, mistakes=mistakes)

# ---------------- BACK ----------------
e("Pull-ups","back","bodyweight",["lats"],["biceps","upper_back","rear_delts"],
 ["Hang from the bar with hands a bit wider than your shoulders, palms facing away.",
  "Pull your shoulder blades down and back before you bend your arms.",
  "Pull until your chin clears the bar, driving your elbows toward your ribs.",
  "Lower yourself under control until your arms are straight."],
 ["Kicking or swinging to get up","Stopping halfway down","Craning the neck to reach the bar"])
e("Chin-ups","back","bodyweight",["lats","biceps"],["upper_back"],
 ["Hang with palms facing you, hands shoulder-width apart.",
  "Brace your core and keep your legs still.",
  "Pull your chest toward the bar until your chin is over it.",
  "Lower slowly to a full hang."],
 ["Half reps at the bottom","Swinging the legs","Shrugging the shoulders up"])
e("Assisted pull-up machine","back","machine",["lats"],["biceps","upper_back"],
 ["Set the counterweight, kneel on the pad and grip the handles wide.",
  "Pull your elbows down to your sides until your chin passes the handles.",
  "Pause briefly, then return slowly to straight arms."],
 ["Using so much assistance the last reps are easy","Rushing the lowering part"])
e("Wide-grip lat pulldown","back","cable",["lats"],["rear_delts","biceps"],
 ["Grip the bar a bit wider than your shoulders, thighs locked under the pad.",
  "Lean back slightly and lift your chest.",
  "Pull the bar to your upper chest by driving your elbows down and back.",
  "Let it rise slowly, about 2 seconds, until your arms are straight."],
 ["Swinging your body back to move the weight","Pulling the bar behind your neck","Shrugging your shoulders up at the top"])
e("Close-grip lat pulldown","back","cable",["lats"],["biceps","upper_back"],
 ["Attach a V-handle and sit with thighs under the pad.",
  "Lean back a little with your chest up.",
  "Pull the handle to your upper chest, elbows close to your body.",
  "Return slowly until your lats are fully stretched."],
 ["Leaning far back and rowing instead","Letting the weight yank your arms up"])
e("Machine lat pulldown","back","machine",["lats"],["biceps","rear_delts"],
 ["Adjust the seat so the handles are just within reach.",
  "Pull the handles down until they reach chest level.",
  "Squeeze your lats for a second at the bottom.",
  "Return slowly to full stretch."],
 ["Moving the torso to help","Cutting the stretch short at the top"])
e("Straight-arm pulldown","back","cable",["lats"],["triceps","abs"],
 ["Stand facing a high cable with a straight bar or rope, arms almost straight.",
  "Hinge slightly forward and brace your core.",
  "Sweep the bar down to your thighs in an arc, keeping elbows soft but fixed.",
  "Return slowly to shoulder height."],
 ["Bending the elbows into a pushdown","Rocking the torso"])
e("Cable wide-grip row","back","cable",["upper_back","lats"],["rear_delts","biceps"],
 ["Sit with feet on the platform and a wide bar in an overhand grip.",
  "Sit tall with a slight bend in the knees.",
  "Row the bar to your lower chest, elbows out at about 45 degrees.",
  "Squeeze your shoulder blades for one second, then extend slowly."],
 ["Rocking back and forth","Rounding the lower back","Letting the shoulders roll forward at the end"])
e("Seated cable row","back","cable",["upper_back","lats"],["biceps","rear_delts"],
 ["Sit with a V-handle, knees slightly bent and chest up.",
  "Pull the handle to your belly, elbows close to your sides.",
  "Squeeze your shoulder blades together.",
  "Let your arms extend fully without rounding your back."],
 ["Using momentum from the hips","Shrugging toward the ears"])
e("Single-arm row","back","dumbbell",["lats","upper_back"],["biceps","rear_delts"],
 ["Place one knee and hand on a bench, back flat.",
  "Hold the dumbbell with a straight arm under your shoulder.",
  "Pull it toward your hip, keeping the elbow close.",
  "Lower it slowly until your arm is straight."],
 ["Twisting the torso to lift","Pulling toward the chest instead of the hip"])
e("Barbell row","back","barbell",["upper_back","lats"],["lower_back","biceps","rear_delts"],
 ["Hinge forward to about 45 degrees with a flat back, bar hanging at your knees.",
  "Brace your core and keep your knees soft.",
  "Row the bar to your lower ribs.",
  "Lower it under control without standing up."],
 ["Standing more upright each rep","Rounding the lower back","Jerking the bar up"])
e("T-bar row","back","barbell",["upper_back","lats"],["lower_back","biceps"],
 ["Straddle the bar and grip the handle with a flat back.",
  "Hinge at the hips with chest up.",
  "Pull the handle to your chest, squeezing your shoulder blades.",
  "Lower slowly to a full stretch."],
 ["Using the legs to heave the weight","Rounding the back"])
e("Chest-supported row","back","machine",["upper_back"],["lats","rear_delts","biceps"],
 ["Lie chest-down on the pad and grip the handles.",
  "Keep your chest on the pad the whole set.",
  "Pull the handles back until your elbows pass your torso.",
  "Return slowly."],
 ["Lifting the chest off the pad","Short, choppy reps"])
e("Machine row","back","machine",["upper_back","lats"],["biceps"],
 ["Set the seat so the handles are at chest height.",
  "Sit tall with your chest against the pad.",
  "Pull the handles back, squeezing your shoulder blades.",
  "Return slowly to straight arms."],
 ["Leaning back to cheat","Letting the weights slam"])
e("Inverted row","back","bodyweight",["upper_back"],["lats","biceps","abs"],
 ["Lie under a bar set at hip height and grip it overhand.",
  "Keep your body straight from heels to head.",
  "Pull your chest up to the bar.",
  "Lower until your arms are straight."],
 ["Sagging hips","Reaching with the chin"])
e("Deadlift","back","barbell",["lower_back","glutes","hamstrings"],["upper_back","quads","forearms"],
 ["Stand with the bar over mid-foot, hands just outside your legs.",
  "Flatten your back and pull the slack out of the bar.",
  "Push the floor away and stand up, keeping the bar close.",
  "Lower by pushing your hips back, then bending your knees."],
 ["Rounding the lower back","Bar drifting away from the legs","Jerking the bar off the floor"])
e("Rack pull","back","barbell",["upper_back","lower_back"],["glutes","hamstrings","forearms"],
 ["Set the bar on pins just below your knees.",
  "Grip it with a flat back and chest up.",
  "Drive your hips forward to stand tall.",
  "Lower it back to the pins under control."],
 ["Leaning back at the top","Rounding the back"])
e("Back extensions","back","bodyweight",["lower_back"],["glutes","hamstrings"],
 ["Set the pad just below your hips and hook your feet.",
  "Cross your arms or hold a plate on your chest.",
  "Lower your torso by bending at the hips with a flat back.",
  "Rise until your body is in a straight line, no higher."],
 ["Hyperextending at the top","Rounding the back on the way down","Going too fast"])
e("Good morning","back","barbell",["hamstrings","lower_back"],["glutes"],
 ["Place a light bar on your upper back, knees soft.",
  "Push your hips back and lower your chest with a flat back.",
  "Stop when you feel a strong hamstring stretch.",
  "Drive your hips forward to stand up."],
 ["Rounding the back","Going too heavy too soon"])
e("Shrugs","back","dumbbell",["traps"],["forearms"],
 ["Stand tall holding dumbbells at your sides.",
  "Lift your shoulders straight up toward your ears.",
  "Hold for one second at the top.",
  "Lower slowly to a full stretch."],
 ["Rolling the shoulders","Bending the elbows","Bouncing the weight"])
e("Barbell shrug","back","barbell",["traps"],["forearms"],
 ["Hold the bar at your thighs with a shoulder-width grip.",
  "Shrug your shoulders straight up.",
  "Pause, then lower under control."],
 ["Rolling the shoulders","Using a short range"])
e("Face pull","back","cable",["rear_delts","upper_back"],["traps"],
 ["Set a rope at face height and grip it with thumbs toward you.",
  "Step back so the stack lifts, arms straight.",
  "Pull the rope toward your eyes, hands ending beside your ears.",
  "Return slowly."],
 ["Pulling to the chest","Leaning back to move more weight"])

# ---------------- CHEST ----------------
e("Barbell bench press","chest","barbell",["chest"],["triceps","front_delts"],
 ["Lie on the bench with eyes under the bar and feet flat.",
  "Squeeze your shoulder blades together and grip slightly wider than your shoulders.",
  "Lower the bar to your mid-chest with elbows at about 45 degrees.",
  "Press it back up over your shoulders."],
 ["Bouncing the bar off the chest","Flaring the elbows straight out","Lifting the hips off the bench"])
e("Incline barbell bench press","chest","barbell",["chest"],["front_delts","triceps"],
 ["Set the bench to about 30 degrees.",
  "Grip slightly wider than shoulders, shoulder blades pinched.",
  "Lower the bar to your upper chest.",
  "Press up and slightly back over your shoulders."],
 ["Bench set too steep","Bouncing off the chest","Losing the arch in the upper back"])
e("Decline bench press","chest","barbell",["chest"],["triceps","front_delts"],
 ["Lock your legs in on a decline bench.",
  "Unrack with straight arms over your chest.",
  "Lower the bar to your lower chest.",
  "Press back up."],
 ["Bouncing the bar","Unracking without a spotter"])
e("Dumbbell bench press","chest","dumbbell",["chest"],["triceps","front_delts"],
 ["Lie on a flat bench with dumbbells at chest level.",
  "Press them up until your arms are straight.",
  "Lower slowly until you feel a stretch across the chest.",
  "Keep your shoulder blades pinched throughout."],
 ["Clanking the dumbbells at the top","Dropping too low and losing shoulder position"])
e("Incline dumbbell press","chest","dumbbell",["chest"],["front_delts","triceps"],
 ["Set the bench to 30 degrees and sit back with dumbbells on your thighs.",
  "Kick them up to your shoulders.",
  "Press up and slightly together.",
  "Lower slowly to upper-chest level."],
 ["Bench too steep, turning it into a shoulder press","Elbows flared at 90 degrees"])
e("Machine chest press","chest","machine",["chest"],["triceps","front_delts"],
 ["Set the seat so the handles line up with your mid-chest.",
  "Press the handles forward until your arms are straight.",
  "Return slowly until you feel a chest stretch.",
  "Keep your back against the pad."],
 ["Seat too high or low","Letting the stack touch between reps"])
e("Incline machine press","chest","machine",["chest"],["front_delts","triceps"],
 ["Adjust the seat so the handles are at upper-chest height.",
  "Press up and forward to straight arms.",
  "Return slowly to a full stretch."],
 ["Shrugging the shoulders","Short range of motion"])
e("Cable fly","chest","cable",["chest"],["front_delts"],
 ["Set both pulleys at shoulder height and step forward into a split stance.",
  "Start with arms wide and elbows slightly bent.",
  "Bring your hands together in front of your chest in a hugging arc.",
  "Return slowly until you feel the stretch."],
 ["Turning it into a press by bending the elbows","Using too much weight to control"])
e("Low-to-high cable fly","chest","cable",["chest"],["front_delts"],
 ["Set both pulleys low and hold the handles with a slight elbow bend.",
  "Sweep your hands up and together to shoulder height.",
  "Squeeze the upper chest.",
  "Lower back slowly."],
 ["Bending the arms too much","Leaning back"])
e("Pec deck","chest","machine",["chest"],["front_delts"],
 ["Set the seat so the handles are at chest height.",
  "Keep a slight bend in your elbows.",
  "Bring the handles together in front of your chest.",
  "Open slowly until you feel a stretch."],
 ["Letting the arms go too far back","Pushing with the hands instead of hugging"])
e("Decline chest fly (machine)","chest","machine",["chest"],["front_delts"],
 ["Set the seat so the handles start at lower-chest height.",
  "Keep a soft bend in the elbows.",
  "Bring the handles together and slightly down.",
  "Return slowly to a full stretch."],
 ["Going too heavy and losing control","Shrugging the shoulders"])
e("Dumbbell fly","chest","dumbbell",["chest"],["front_delts"],
 ["Lie on a bench with dumbbells above your chest, palms facing in.",
  "Lower them out wide with a slight elbow bend.",
  "Stop when you feel a deep chest stretch.",
  "Bring them back together in the same arc."],
 ["Straightening the arms fully","Going too deep with heavy weight"])
e("Dips","chest","bodyweight",["chest","triceps"],["front_delts"],
 ["Support yourself on the bars with straight arms.",
  "Lean slightly forward.",
  "Lower until your upper arms are about parallel to the floor.",
  "Push back up to straight arms."],
 ["Going too deep and straining the shoulders","Swinging the legs"])
e("Push-ups","chest","bodyweight",["chest"],["triceps","front_delts","abs"],
 ["Place your hands slightly wider than your shoulders.",
  "Keep your body in a straight line.",
  "Lower your chest to just above the floor.",
  "Push back up to straight arms."],
 ["Sagging hips","Flaring the elbows straight out","Partial reps"])
e("Smith machine bench press","chest","machine",["chest"],["triceps","front_delts"],
 ["Set the bench so the bar lowers to your mid-chest.",
  "Unhook the bar with a grip slightly wider than your shoulders.",
  "Lower it to your chest under control.",
  "Press back up."],
 ["Bench in the wrong position","Bouncing off the chest"])
e("Landmine press","chest","barbell",["chest","front_delts"],["triceps"],
 ["Hold the end of a landmine bar at your chest.",
  "Stand in a split stance and brace.",
  "Press the bar up and forward to straight arms.",
  "Lower it back to your chest."],
 ["Leaning back","Locking the knees"])
