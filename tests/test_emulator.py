import contextlib
import json
import os

from tabi.emulator import detect_conflicts, detect_hijacks

from tabi.rib import EmulatedRIB

PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "resources/conflict_annotation/inputs")

RIB = {
    "type": "table_dump_v2",
    "timestamp": 1451601234.0,
    "entries": [{
        "peer_ip": "11.33.55.77",
        "peer_as": 99999.0,
        "originated_timestamp": 0.0,
        "as_path": "22 333 4444 55555"
    }],
    "prefix": "1.2.3.0/24"
}

UPDATE = {
    "type": "update",
    "timestamp": 1451606698.0,
    "peer_as": 11111.0,
    "peer_ip": "22.44.66.88",
    "as_path": "1111 2222 3333",
    "announce": ["1.2.3.0/25"],
    "withdraw": []
}

WITHDRAW = {
    "type": "update",
    "timestamp": 1451607000.0,
    "peer_as": 99999.0,
    "peer_ip": "11.33.55.77",
    "announce": [],
    "withdraw": ["1.2.3.0/24"]
}

EXPECTED = {
    'timestamp': 1451606698.0,
    'collector': 'collector',
    'peer_as': 11111,
    'peer_ip': "22.44.66.88",
    'announce': {
        'type': 'U',
        'prefix': '1.2.3.0/25',
        'asn': 3333,
        'as_path': '1111 2222 3333'
    },
    'conflict_with': {
        'asn': 55555,
        'prefix': '1.2.3.0/24'
    },
    'asn': 55555
}


@contextlib.contextmanager
def dict_opener(line):
    yield [json.dumps(line)]


def test_detect_conflicts_boundary_1():
    """Check if conflicts detection fails on empty file list"""
    try:
        conflicts = detect_conflicts("collector", [])
        conflicts.next()
        raise Exception("Should raise an exception on empty file list")
    except ValueError as error:
        assert error.message == "no bviews were loaded"


def test_detect_conflicts_boundary_2():
    """Check if conflicts detection fails on a file with partial data"""
    try:
        conflicts = detect_conflicts("collector", [{"type": "table_dump_v2"}], opener=dict_opener)
        conflicts.next()
        raise Exception("Should not find any conflict")
    except StopIteration:
        pass


def test_detect_conflicts_rib_update():
    """Check if conflicts detection works for a conflict between rib and update files"""
    conflicts = detect_conflicts("collector", [RIB, UPDATE], opener=dict_opener)
    conflict = conflicts.next()
    assert conflict == EXPECTED
    try:
        conflicts.next()
        raise Exception("Should find only one conflict")
    except StopIteration:
        pass


def test_detect_conflicts_rib_withdraw_update():
    """Check if conflicts detection works with withdrawals"""
    conflicts = detect_conflicts("collector", [RIB, WITHDRAW, UPDATE], opener=dict_opener)
    try:
        conflicts.next()
        raise Exception("Should not find any conflict")
    except StopIteration:
        pass


def test_detect_conflicts_rib_only():
    """Check if conflicts detection works for a conflict within rib file"""
    rib1 = RIB
    rib2 = {
        "entries": [{
            "peer_ip": "22.44.66.88",
            "peer_as": 88888.0,
            "originated_timestamp": 1451605511.0,
            "as_path": "22 333 4444 66666"
        }],
        "type": "table_dump_v2",
        "timestamp": 1451605511.0,
        "prefix": "1.2.3.0/24"
    }
    conflicts = detect_conflicts("collector", [rib1, rib2], opener=dict_opener)
    conflict = conflicts.next()
    expected = {
        'timestamp': 1451601234.0,
        'collector': 'collector',
        'peer_as': 99999,
        'peer_ip': "11.33.55.77",
        'announce': {
            'type': 'F',
            'prefix': '1.2.3.0/24',
            'asn': 55555,
            'as_path': '22 333 4444 55555'
        },
        'conflict_with': {
            'asn': 66666,
            'prefix': '1.2.3.0/24'
        },
        'asn': 66666
    }
    assert conflict == expected
    # try:
    #     conflicts.next()
    #     raise Exception("Should only return one conflict")
    # except StopIteration:
    #     pass


def test_detect_multiple_conflicts():
    """Check if conflicts detection works in subsequent calls"""
    rib = EmulatedRIB()
    conflicts = detect_conflicts("collector", [RIB], opener=dict_opener, rib=rib)
    try:
        conflicts.next()
    except StopIteration:
        pass
    conflicts = detect_conflicts("collector", [UPDATE], opener=dict_opener, rib=rib)
    conflict = conflicts.next()
    assert conflict == EXPECTED
    try:
        conflicts.next()
        raise Exception("Should find only one conflict")
    except StopIteration:
        pass


def test_detect_hijacks_boundary_1():
    """Check if conflicts detection fails on empty file list"""
    try:
        hijacks = detect_hijacks("collector", [])
        hijacks.next()
        raise Exception("Should raise an exception on empty file list")
    except ValueError as error:
        assert error.message == "no bviews were loaded"


def test_detect_hijacks_rib_update():
    """Check if conflicts detection works for a conflict between rib and update files"""
    conflicts = detect_hijacks("collector", [RIB, UPDATE], opener=dict_opener)
    conflict = conflicts.next()
    expected = EXPECTED.copy()
    expected['type'] = 'ABNORMAL'
    assert conflict == expected
    try:
        conflicts.next()
        raise Exception("Should find only one conflict")
    except StopIteration:
        pass


def test_detect_hijacks():
    """Check if hijacks detection between two RIB records """

    irr_org_file = os.path.join(PATH, "organisations_file")
    irr_mnt_file = os.path.join(PATH, "maintainers_file")
    irr_ro_file = os.path.join(PATH, "ro_file")
    rpki_roa_file = os.path.join(PATH, "roa_file")

    rib1 = RIB

    rib2 = {
        "entries": [{
            "peer_ip": "22.44.66.88",
            "peer_as": 99999.0,
            "originated_timestamp": 1451605511.0,
            "as_path": "22 333 4444 66666"
        }],
        "type": "table_dump_v2",
        "timestamp": 1451605511.0,
        "prefix": "1.2.3.0/24"
    }

    conflicts = detect_hijacks("collector", [rib1, rib2],
                               irr_org_file=irr_org_file,
                               irr_mnt_file=irr_mnt_file,
                               irr_ro_file=irr_ro_file,
                               rpki_roa_file=rpki_roa_file,
                               opener=dict_opener)

    conflict = conflicts.next()
    assert conflict["type"] == "ABNORMAL"

if __name__ == '__main__':
    test_detect_hijacks()
