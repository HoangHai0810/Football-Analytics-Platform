import uuid

def normalize_event(raw_event: dict, match_id: int) -> dict:
    """
    Normalizes a raw StatsBomb event record to match the platform's schema.
    Extracts pitch coordinates, timestamps, actors, and outcomes.
    """
    try:
        raw_id = raw_event.get("id")
        event_id = str(uuid.UUID(str(raw_id))) if raw_id else str(uuid.uuid4())
    except (ValueError, AttributeError):
        event_id = str(uuid.uuid4())

    player = raw_event.get("player") or {}
    player_id = int(player.get("id", 0)) if isinstance(player, dict) else 0

    team = raw_event.get("team") or {}
    team_id = int(team.get("id", 0)) if isinstance(team, dict) else 0

    event_type_obj = raw_event.get("type") or {}
    event_type = event_type_obj.get("name", "UNKNOWN").upper() if isinstance(event_type_obj, dict) else str(raw_event.get("type", "UNKNOWN")).upper()

    minute = int(raw_event.get("minute", 0))
    second = int(raw_event.get("second", 0))

    location = raw_event.get("location")
    x = 0.0
    y = 0.0
    if isinstance(location, (list, tuple)) and len(location) > 0:
        if location[0] is not None:
            try: x = float(location[0])
            except (ValueError, TypeError): x = 0.0
        if len(location) > 1 and location[1] is not None:
            try: y = float(location[1])
            except (ValueError, TypeError): y = 0.0

    end_x = None
    end_y = None
    outcome = "SUCCESS"

    # Specific event handling
    if "pass" in raw_event and isinstance(raw_event["pass"], dict):
        pass_data = raw_event["pass"]
        end_loc = pass_data.get("end_location")
        if isinstance(end_loc, (list, tuple)) and len(end_loc) > 0:
            if end_loc[0] is not None:
                try: end_x = float(end_loc[0])
                except (ValueError, TypeError): pass
            if len(end_loc) > 1 and end_loc[1] is not None:
                try: end_y = float(end_loc[1])
                except (ValueError, TypeError): pass
        if pass_data.get("outcome"):
            outcome = str(pass_data["outcome"].get("name", "FAIL")).upper()

    elif "shot" in raw_event and isinstance(raw_event["shot"], dict):
        shot_data = raw_event["shot"]
        end_loc = shot_data.get("end_location")
        if isinstance(end_loc, (list, tuple)) and len(end_loc) > 0:
            if end_loc[0] is not None:
                try: end_x = float(end_loc[0])
                except (ValueError, TypeError): pass
            if len(end_loc) > 1 and end_loc[1] is not None:
                try: end_y = float(end_loc[1])
                except (ValueError, TypeError): pass
        if shot_data.get("outcome"):
            outcome = str(shot_data["outcome"].get("name", "OFF_TARGET")).upper()

    return {
        "event_id": event_id,
        "match_id": match_id,
        "player_id": player_id,
        "team_id": team_id,
        "event_type": event_type,
        "minute": minute,
        "second": second,
        "x": x,
        "y": y,
        "end_x": end_x,
        "end_y": end_y,
        "outcome": outcome,
    }
