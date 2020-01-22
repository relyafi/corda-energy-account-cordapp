"use strict";

let Alert = ReactBootstrap.Alert;
let Accordion = ReactBootstrap.Accordion;
let Button = ReactBootstrap.Button;
let ButtonGroup = ReactBootstrap.ButtonGroup;
let Card = ReactBootstrap.Card;
let Col = ReactBootstrap.Col;
let Container = ReactBootstrap.Container;
let Form = ReactBootstrap.Form;
let InputGroup = ReactBootstrap.InputGroup;
let ListGroup = ReactBootstrap.ListGroup;
let Modal = ReactBootstrap.Modal;
let NavBar = ReactBootstrap.Navbar;
let Row = ReactBootstrap.Row;
let Table = ReactBootstrap.Table;
let Tab = ReactBootstrap.Tab;
let Tabs = ReactBootstrap.Tabs;
let Text = ReactBootstrap.Text;

function formatDate(string){
    var options = { dateStyle: 'long', timeStyle: 'medium' };
    return new Date(string).toLocaleString([],options);
}

function getIdentityDisplayName(X500Name) {
    return X500Name.match("(?<=O=).*(?=, L=)")[0]
}

class CustomerFormRow extends React.Component {
    constructor(props) {
        super(props);

        this.handleFieldChange = this.handleFieldChange.bind(this);
    }

    render() {
        return (
            <Form.Group as={Row}>
                <Form.Label xs="auto"
                            className="custDetailsLabel font-weight-bolder"
                            column
                            span="false">{this.props.label}
                </Form.Label>
                <Col>
                    <Form.Control placeholder={this.props.readonly ? "" : this.props.placeholder}
                                  disabled={this.props.readonly==true}
                                  plaintext={this.props.readonly==true}
                                  onChange={this.handleFieldChange}
                                  name={this.props.name}
                                  value={this.props.value}
                                  maxLength={this.props.maxLength}/>
                </Col>
            </Form.Group>
        )
    }

    handleFieldChange(event) {
        this.props.onChange(event.target.name, event.target.value);
    }
}

class CustomerForm extends React.Component {
    constructor(props) {
        super(props);

        this.handleFieldChange = this.handleFieldChange.bind(this);
    }

    render() {
        return (
            <Form>
                <CustomerFormRow name="firstName"
                                 label="First Name"
                                 placeholder="Required"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.firstName}
                                 onChange={this.props.onChange}/>
                <CustomerFormRow name="lastName"
                                 label="Last Name"
                                 placeholder="Required"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.lastName}
                                 onChange={this.props.onChange}/>
                <CustomerFormRow name="dateOfBirth"
                                 label="Date Of Birth"
                                 placeholder="YYYY-MM-DD"
                                 maxLength="10"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.dateOfBirth}
                                 onChange={this.props.onChange}/>
                <CustomerFormRow name="address"
                                 label="Address"
                                 placeholder="Required"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.address}
                                 onChange={this.props.onChange}/>
                <CustomerFormRow name="phoneNumber"
                                 label="Phone Number"
                                 placeholder="Optional"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.phoneNumber}
                                 onChange={this.props.onChange}/>
                <CustomerFormRow name="email"
                                 label="Email"
                                 placeholder="Optional"
                                 readonly={this.props.mode=="View"}
                                 value={this.props.customerDetails.email}
                                 onChange={this.props.onChange}/>
            </Form>
        )
    }

    handleFieldChange(event) {
        this.props.onChange(event.target.name, event.target.value);
    }
}

class AccountViewDialog extends React.Component {
    constructor(props) {
        super(props);

        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "View"}>
                    <Modal.Header>
                        <Modal.Title>Account Details</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        <Tabs defaultActiveKey="details">
                            <Tab className="mt-2 ml-2"
                                 eventKey="details"
                                 title="Customer Details">
                                <CustomerForm
                                    customerDetails={this.props.accountDetails.customerDetails}
                                    mode={this.props.actionState}
                                    onChange={this.onChange}/>
                            </Tab>
                            <Tab className="mt-2"
                                 eventKey="readings"
                                 title="Meter Readings">
                                <MeterReadingTable
                                    meterReadings={this.props.accountDetails.meterReadings} />
                            </Tab>
                        </Tabs>
                    </Modal.Body>
                    <Modal.Footer>
                          <Button onClick={this.handleClose}>
                              Close
                          </Button>
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    onChange(field, value) {
        // No-op - view only
    }

    handleClose() {
        this.props.onClose();
    }
}

class MeterReadingTable extends React.Component {
    constructor(props) {
        super(props)
    }

    render() {
        return (
            <Table striped>
                <thead>
                    <tr>
                    <th style={{width:"40ch"}}>Date</th>
                    <th>Reading</th>
                    </tr>
                </thead>
                <tbody>
                { this.props.meterReadings.map(
                    reading =>
                        <tr>
                          <td>{formatDate(reading.first)}</td>
                          <td>{String(reading.second).padStart(5, '0')}</td>
                        </tr>
                )}
                </tbody>
            </Table>
        )
    }
}
