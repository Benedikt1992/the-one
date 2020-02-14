from src.reader.message_processing_report_reader import MessageProcessingReportReader


class NodeLoad:
    def __init__(self, processing_report: MessageProcessingReportReader):
        self.processing_report = processing_report

    def load_distribution_by_hostgroup(self):
        groups = self.processing_report.get_host_groups()
        # TODO make boxplots of incoming and outgoing distribution per node group
        print(groups)
